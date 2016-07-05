package peds.akka.stream

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
import akka.NotUsed
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.agent.Agent
import akka.event.LoggingReceive
import akka.pattern.{ask, pipe}
import akka.stream.actor._
import akka.stream.scaladsl._
import akka.stream.{FlowShape, Graph, Materializer}
import akka.util.Timeout
import com.codahale.metrics.{Metric, MetricFilter}
import nl.grons.metrics.scala.Meter
import peds.akka.metrics.InstrumentedActor



/**
  * Created by rolfsd on 4/2/16.
  */
object MaxInFlightProcessorAdapter {
  def fixedProcessorFlow[I, O: ClassTag](
    name: String,
    maxInFlight: Int
  )(
    workerPF: PartialFunction[Any, ActorRef]
  )(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Flow[I, O, NotUsed] = {
    val publisherProps = ProcessorPublisher.props[O]
    val g = processorGraph[I, O](
      name = name,
      adapterPropsFromOutlet = ( publisher: ActorRef ) => {
        MaxInFlightProcessorAdapter.fixedProps( maxInFlight, publisher )( workerPF )
      },
      outletProps = publisherProps
    )

    import StreamMonitor._
    Flow.fromGraph( g ).watchFlow( Symbol(name) )
  }

  def elasticProcessorFlow[I, O: ClassTag](
    name: String,
    maxInFlightCpuFactor: Double
  )(
    workerPF: PartialFunction[Any, ActorRef]
  )(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Flow[I, O, NotUsed] = {
    val publisherProps = ProcessorPublisher.props[O]
    val g = processorGraph[I, O](
      name = name,
      adapterPropsFromOutlet = (publisher: ActorRef) => {
        MaxInFlightProcessorAdapter.elasticProps( maxInFlightCpuFactor, publisher )( workerPF )
      },
      outletProps = publisherProps
    )

    import StreamMonitor._
    Flow.fromGraph( g ).watchFlow( Symbol(name) )
  }

  def processorGraph[I, O](
    name: String,
    adapterPropsFromOutlet: ActorRef => Props,
    outletProps: Props
  )(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Graph[FlowShape[I, O], NotUsed] = {
    GraphDSL.create() { implicit b =>
      val ( outletRef, outlet ) = {
        Source.actorPublisher[O]( outletProps ).named( s"${name}-processor-outlet" )
        .toMat( Sink.asPublisher(false) )( Keep.both )
        .run()
      }

      val outletShape = b.add( Source fromPublisher[O] outlet )
      val inletShape = b.add(
        Sink.actorSubscriber[I]( adapterPropsFromOutlet( outletRef ) ).named( s"${name}-processor-inlet")
      )

      FlowShape( inletShape.in, outletShape.out )
    }
  }


  def fixedProps( maxInFlightMessages: Int, outletRef: ActorRef )( workerPF: PartialFunction[Any, ActorRef] ): Props = {
    Props(
      new MaxInFlightProcessorAdapter with TopologyProvider {
        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
        override val maxInFlight: Int = maxInFlightMessages
        override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
      }
    )
  }

  def elasticProps(
    maxInFlightMessagesCpuFactor: Double,
    outletRef: ActorRef
  )(
    workerPF: PartialFunction[Any, ActorRef]
  ): Props = {
    Props(
      new MaxInFlightProcessorAdapter with TopologyProvider {
        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
        override val maxInFlightCpuFactor: Double = maxInFlightMessagesCpuFactor
        override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
      }
    )
  }


  trait TopologyProvider { outer: Actor =>
    def workerFor: PartialFunction[Any, ActorRef]
    def outlet( implicit context: ActorContext ): ActorRef
    def maxInFlight: Int = math.floor( Runtime.getRuntime.availableProcessors() * maxInFlightCpuFactor ).toInt
    def maxInFlightCpuFactor: Double = 1.0
    implicit def timeout: Timeout = Timeout( 10.seconds )
    implicit def inFlightExecutor: ExecutionContext = context.system.dispatchers.lookup( "in-flight-dispatcher" )
  }


  case class DetectionJob( destination: ActorRef, startNanos: Long = System.nanoTime() )


  final case class DeadWorkerError private[MaxInFlightProcessorAdapter]( deadWorker: ActorRef )
  extends IllegalStateException( s"Flow Processor notified of worker death: [${deadWorker}]" )
}

class MaxInFlightProcessorAdapter extends ActorSubscriber with InstrumentedActor with ActorLogging {
  outer: MaxInFlightProcessorAdapter.TopologyProvider =>

  override def preStart(): Unit = initializeMetrics()

  val submissionMeter: Meter = metrics meter "submission"
  val publishMeter: Meter = metrics meter "publish"

  def initializeMetrics(): Unit = {
    stripLingeringMetrics()
    metrics.gauge( "outstanding" ) { outstanding }
  }

  def stripLingeringMetrics(): Unit = {
    metrics.registry.removeMatching(
      new MetricFilter {
        override def matches( name: String, metric: Metric ): Boolean = {
          name.contains( classOf[MaxInFlightProcessorAdapter].getName ) && name.contains( "outstanding" )
        }
      }
    )
  }


  type InFlightMessage = Future[Any]
  var outstanding: Agent[Set[InFlightMessage]] = Agent( Set.empty[InFlightMessage] )( outer.inFlightExecutor )

  override protected def requestStrategy: RequestStrategy = {
    new MaxInFlightRequestStrategy( max = outer.maxInFlight ) {
      override def inFlightInternally: Int = {
        val inflight = outstanding.future map { o =>
          log.info( "INFLIGHT: inFlightInternally=[{}]", o.size )
          o.size
        }
        scala.concurrent.Await.result( inflight, 2.seconds )
      }
    }
  }


  override def receive: Receive = LoggingReceive { around( withSubscriber(outer.outlet) ) }

  def withSubscriber( outlet: ActorRef ): Receive = {
    case next @ ActorSubscriberMessage.OnNext( message ) if outer.workerFor isDefinedAt message => {
      submissionMeter.mark()
      val worker = outer workerFor message
      context watch worker

      val inflight = ( worker ? message ) map { result =>
        log.info( "INFLIGHT: worker processed [{}] = [{}]", message, result )
        result
      }

      outstanding alter { _ + inflight }

      inflight pipeTo outer.outlet
      inflight onComplete {
        case Success( msg ) => {
          log.info(
            "INFLIGHT: altering to remove future:[{}] from outstanding in flight since received worker result:[{}]",
            inflight.##,
            msg
          )
          outstanding alter { o =>
            log.info( "INFLIGHT: in-agent (via success) removal of future:[{}]", inflight.## )
            o - inflight
          }
        }

        case Failure( ex ) => {
          log.error( ex, "In Flight process failed - removing from outstanding InFlight future:[{}]", inflight.## )
          outstanding alter { o =>
            log.info( "INFLIGHT: in-agent (via failure) removal of future:[{}]", inflight.## )
            o - inflight
          }
        }
      }
    }

    case ActorSubscriberMessage.OnComplete => {
      val remaining = {
        outstanding.future
        .flatMap { outs => Future sequence outs }
        .map { outs =>
          log.info( "INFLIGHT: remaining outstanding[{}] completed so propagating OnComplete", outs.size )
          ActorSubscriberMessage.OnComplete
        }
      }
      remaining pipeTo outlet
    }

    case onError: ActorSubscriberMessage.OnError => outlet ! onError

    case Terminated( deadWorker ) => {
      log.error( "Flow Processor notified of worker death: [{}]", deadWorker )
      //todo is this response appropriate for spotlight and generally?
//      outer.destinationPublisher ! ActorSubscriberMessage.OnError( ProcessorAdapter.DeadWorkerError(deadWorker) )
    }
  }
}
