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
    .named( s"fixed-processor-${name}" )

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
    .named( s"elastic-processor-${name}")

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
    Props( new Fixed(maxInFlightMessages, outletRef)(workerPF) )
  }

  private class Fixed(
    override val maxInFlight: Int,
    outletRef: ActorRef
  )(
    workerPF: PartialFunction[Any, ActorRef]
  ) extends MaxInFlightProcessorAdapter with TopologyProvider {
    log.info( "[{}][{}] setting watch on outlet:[{}]", self.path, this.##, outletRef.path )
    context watch outletRef
    override val workerFor: PartialFunction[Any, ActorRef] = workerPF
    override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
  }


  def elasticProps(
    maxInFlightMessagesCpuFactor: Double,
    outletRef: ActorRef
  )(
    workerPF: PartialFunction[Any, ActorRef]
  ): Props = Props( new Elastic(maxInFlightMessagesCpuFactor, outletRef)(workerPF) )

  private class Elastic(
    override val maxInFlightCpuFactor: Double,
    outletRef: ActorRef
  )(
    workerPF: PartialFunction[Any, ActorRef]
  ) extends MaxInFlightProcessorAdapter with TopologyProvider {
    log.info( "[{}][{}] setting watch on outlet:[{}]", self.path, this.##, outletRef.path )
    context watch outletRef
    override val workerFor: PartialFunction[Any, ActorRef] = workerPF
    override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
  }


  trait TopologyProvider { outer: Actor with ActorLogging =>
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
          log.debug( "MaxInFlightProcessorAdapter[{}] inFlightInternally=[{}]", this.##, o.size )
          o.size
        }
        scala.concurrent.Await.result( inflight, 2.seconds )
      }
    }
  }


  override def receive: Receive = LoggingReceive { around( withSubscriber(outer.outlet) orElse maintenance ) }

  def withSubscriber( outlet: ActorRef ): Receive = {
    case next @ ActorSubscriberMessage.OnNext( message ) if outer.workerFor isDefinedAt message => {
      submissionMeter.mark()
      val worker = outer workerFor message
      context watch worker

      val inflight = ( worker ? message ) map { result =>
        log.debug( "MaxInFlightProcessorAdapter[{}]: worker processed [{}] = [{}]", this.##, message, result )
        result
      }

      outstanding alter { o =>
        val altered = o + inflight
        log.debug(
          "MaxInFlightProcessorAdapter[{}]: in-agent addition of future:[{}] resulting outstanding=[{}]",
          this.##,
          inflight.##,
          altered.size
        )
        altered
      }

      inflight pipeTo outer.outlet
      inflight onComplete {
        case Success( msg ) => {
          outstanding alter { o =>
            val altered = o - inflight
            log.debug(
              "MaxInFlightProcessorAdapter[{}]: in-agent (via worker success) removal of future:[{}] resulting outstanding=[{}]",
              this.##,
              inflight.##,
              altered.size
            )
            altered
          }
        }

        case Failure( ex ) => {
          outstanding alter { o =>
            val altered = o - inflight
            log.debug(
              "MaxInfFlightProcessorAdapter[{}]: in-agent (via worker failure) removal of future:[{}]",
              this.##,
              inflight.##,
              altered.size
            )
            altered
          }
        }
      }
    }

    case ActorSubscriberMessage.OnComplete => {
      val complete = {
        outstanding.future
        .flatMap { outs: Set[Future[Any]] => Future sequence outs }
        .map { outs: Set[Any] =>
          log.debug(
            "MaxInfFlightProcessorAdapter[{}]: outstanding [{}] completed so propagating OnComplete",
            this.##,
            outstanding.get.size
          )
          ActorSubscriberMessage.OnComplete
        }
      }

      complete foreach { _ =>
        val outs = scala.concurrent.Await.result( outstanding.future(), 2.seconds )
        log.debug( "MaxInFlightProcessorAdapter[{}]: COMPLETED with [{}] outstanding tasks", this.##, outs.size )
      }

      complete pipeTo outlet
    }

    case onError: ActorSubscriberMessage.OnError => outlet ! onError
  }

  val maintenance: Receive = {
    case Terminated( deadOutlet ) if deadOutlet == outer.outlet => {
      log.error(
        "MaxInFlightProcessorAdapter[{}][{}] notified of dead outlet:[{}] - stopping processor",
        self.path,
        this.##,
        outer.outlet.path
      )
      context stop self
    }

    case Terminated( deadWorker ) => {
      log.error( "MaxInFlightProcessorAdapter[{}][{}] notified of dead worker:[{}]", self.path, this.##, deadWorker.path )
      //todo is this response appropriate for spotlight and generally?
      outer.outlet ! ActorSubscriberMessage.OnError( MaxInFlightProcessorAdapter.DeadWorkerError(deadWorker) )
    }
  }
}
