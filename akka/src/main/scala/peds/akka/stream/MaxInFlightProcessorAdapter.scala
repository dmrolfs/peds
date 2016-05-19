package peds.akka.stream

import akka.NotUsed

import scala.reflect.ClassTag
import akka.actor.{ActorContext, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.event.LoggingReceive
import akka.stream.actor._
import akka.stream.scaladsl._
import akka.stream.{FlowShape, Graph, Materializer}
import com.codahale.metrics.{Metric, MetricFilter}
import nl.grons.metrics.scala.{Meter, MetricName}
import peds.akka.metrics.InstrumentedActor
import peds.commons.collection.BloomFilter


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


  trait TopologyProvider {
    def workerFor: PartialFunction[Any, ActorRef]
    def outlet( implicit context: ActorContext ): ActorRef
    def maxInFlight: Int = math.floor( Runtime.getRuntime.availableProcessors() * maxInFlightCpuFactor ).toInt
    def maxInFlightCpuFactor: Double = 1.0
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


  var outstanding: Int = 0

  override protected def requestStrategy: RequestStrategy = {
    new MaxInFlightRequestStrategy( max = outer.maxInFlight ) {
      override def inFlightInternally: Int = outstanding
    }
  }


  override def receive: Receive = LoggingReceive {
    around( withSubscriber(outer.outlet) orElse publish(outer.outlet) )
  }

  def withSubscriber( outlet: ActorRef ): Receive = {
    case next @ ActorSubscriberMessage.OnNext( message ) if outer.workerFor.isDefinedAt( message ) => {
      submissionMeter.mark()
      outstanding += 1
      val worker = outer workerFor message
      context watch worker
      worker ! message
    }

    case ActorSubscriberMessage.OnComplete => outlet ! ActorSubscriberMessage.OnComplete

    case onError: ActorSubscriberMessage.OnError => outlet ! onError

    case Terminated( deadWorker ) => {
      log.error( "Flow Processor notified of worker death: [{}]", deadWorker )
      //todo is this response appropriate for spotlight and generally?
//      outer.destinationPublisher ! ActorSubscriberMessage.OnError( ProcessorAdapter.DeadWorkerError(deadWorker) )
    }
  }

  def publish( outlet: ActorRef ): Receive = {
    case message => {
      publishMeter.mark()
      outstanding -= 1
      outlet ! message
    }
  }
}
