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
object ProcessorAdapter {
  def fixedProcessorFlow[I, O: ClassTag](
    maxInFlight: Int,
    label: Symbol
  )(
    workerPF: PartialFunction[Any, ActorRef]
  )(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Flow[I, O, NotUsed] = {
    val publisherProps = ProcessorPublisher.props[O]
    val g = processorGraph[I, O](
      adapterPropsFromPublisher = ( publisher: ActorRef ) => { ProcessorAdapter.fixedProps( maxInFlight, publisher )( workerPF ) },
      publisherProps = publisherProps,
      label = label
    )

    import StreamMonitor._
    Flow.fromGraph( g ).watchFlow( label )
  }

  def elasticProcessorFlow[I, O: ClassTag](
    maxInFlightCpuFactor: Double,
    label: Symbol
  )(
    workerPF: PartialFunction[Any, ActorRef]
  )(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Flow[I, O, NotUsed] = {
    val publisherProps = ProcessorPublisher.props[O]
    val g = processorGraph[I, O](
      adapterPropsFromPublisher = (publisher: ActorRef) => {
        ProcessorAdapter.elasticProps( maxInFlightCpuFactor, publisher )( workerPF )
      },
      publisherProps = publisherProps,
      label = label
    )

    import StreamMonitor._
    Flow.fromGraph( g ).watchFlow( label )
  }

  def processorGraph[I, O](
    adapterPropsFromPublisher: ActorRef => Props,
    publisherProps: Props,
    label: Symbol
  )(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Graph[FlowShape[I, O], NotUsed] = {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val ( publisherRef, publisher ) = {
        Source.actorPublisher[O]( publisherProps ).named( s"${label.name}-processor-egress" )
        .toMat( Sink.asPublisher(false) )( Keep.both )
        .run()
      }

      val processorPublisher = b.add( Source fromPublisher[O] publisher )
//      val egress = b.add( Flow[O] map { identity } )

//      processorPublisher ~> egress

      val adapter = b.add(
        Sink.actorSubscriber[I]( adapterPropsFromPublisher( publisherRef ) ).named( s"${label.name}-processor-ingress")
      )

//      FlowShape( adapter.in, egress.out )
      FlowShape( adapter.in, processorPublisher.out )
    }
  }


  def fixedProps( maxInFlightMessages: Int, publisher: ActorRef )( workerPF: PartialFunction[Any, ActorRef] ): Props = {
    Props(
      new ProcessorAdapter with TopologyProvider {
        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
        override val maxInFlight: Int = maxInFlightMessages
        override def destinationPublisher( implicit ctx: ActorContext ): ActorRef = publisher
      }
    )
  }

  def elasticProps(
    maxInFlightMessagesCpuFactor: Double,
    publisher: ActorRef
  )(
    workerPF: PartialFunction[Any, ActorRef]
  ): Props = {
    Props(
      new ProcessorAdapter with TopologyProvider {
        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
        override val maxInFlightCpuFactor: Double = maxInFlightMessagesCpuFactor
        override def destinationPublisher( implicit ctx: ActorContext ): ActorRef = publisher
      }
    )
  }


  trait TopologyProvider {
    def workerFor: PartialFunction[Any, ActorRef]
    def destinationPublisher( implicit context: ActorContext ): ActorRef
    def maxInFlight: Int = math.floor( Runtime.getRuntime.availableProcessors() * maxInFlightCpuFactor ).toInt
    def maxInFlightCpuFactor: Double = 1.0
  }


  case class DetectionJob( destination: ActorRef, startNanos: Long = System.nanoTime() )


  final case class DeadWorkerError private[ProcessorAdapter]( deadWorker: ActorRef )
  extends IllegalStateException( s"Flow Processor notified of worker death: [${deadWorker}]" )
}

class ProcessorAdapter extends ActorSubscriber with InstrumentedActor with ActorLogging {
  outer: ProcessorAdapter.TopologyProvider =>

  override def preStart(): Unit = initializeMetrics()

  override lazy val metricBaseName: MetricName = MetricName( classOf[ProcessorAdapter] )
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
          name.contains( classOf[ProcessorAdapter].getName ) && name.contains( "outstanding" )
        }
      }
    )
  }


  var outstanding: Int = 0

//  val seenMax: Int = 1000000
//  var seen: BloomFilter[Any] = BloomFilter[Any]( maxFalsePosProbability = 0.01, seenMax )
  def reportDuplicates( toBePublished: Iterable[Any] ): Unit = {
//    toBePublished foreach { tbp =>
//      if ( seen.size == seenMax ) log.warning( "[ProcessAdapter] SEEN filled" )
//      if ( seen.size < seenMax ) {
//        if ( seen has_? tbp ) log.warning( "[ProcessorAdapter] Possible duplicate: [{}]", tbp)
//        else seen += tbp
//      }
//    }
  }

  override protected def requestStrategy: RequestStrategy = {
    new MaxInFlightRequestStrategy( max = outer.maxInFlight ) {
      override def inFlightInternally: Int = outstanding
    }
  }


  override def receive: Receive = LoggingReceive {
    around( withSubscriber(outer.destinationPublisher) orElse publish(outer.destinationPublisher) )
  }

  def withSubscriber( subscriber: ActorRef ): Receive = {
    case next @ ActorSubscriberMessage.OnNext( message ) if outer.workerFor.isDefinedAt( message ) => {
      reportDuplicates( Seq(message) )
      submissionMeter.mark()
      outstanding += 1
      val worker = outer workerFor message
      context watch worker
      worker ! message
    }

    case ActorSubscriberMessage.OnComplete => outer.destinationPublisher ! ActorSubscriberMessage.OnComplete

    case onError: ActorSubscriberMessage.OnError => outer.destinationPublisher ! onError

    case Terminated( deadWorker ) => {

      log.error( "Flow Processor notified of worker death: [{}]", deadWorker )
      //todo is this response appropriate for spotlight and generally?
//      outer.destinationPublisher ! ActorSubscriberMessage.OnError( ProcessorAdapter.DeadWorkerError(deadWorker) )
    }
  }

  def publish( subscriber: ActorRef ): Receive = {
    case message => {
      publishMeter.mark()
      outstanding -= 1
      subscriber ! message
    }
  }
}
