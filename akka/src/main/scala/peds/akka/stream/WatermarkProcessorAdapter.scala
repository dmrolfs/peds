//package peds.akka.stream
//
//import scala.reflect.ClassTag
//import akka.actor.{ActorContext, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
//import akka.event.LoggingReceive
//import akka.NotUsed
//import akka.stream.actor._
//import akka.stream.scaladsl._
//import akka.stream.{FlowShape, Graph, Materializer}
//import com.codahale.metrics.{Metric, MetricFilter}
//import nl.grons.metrics.scala.Meter
//import peds.akka.metrics.InstrumentedActor
//
//
///**
//  * Created by rolfsd on 4/2/16.
//  */
//object WatermarkProcessorAdapter {
//  def fixedProcessorFlow[I, O: ClassTag](
//    name: String,
//    highWatermark: Int,
//    lowWatermark: Int
//  )(
//    workerPF: PartialFunction[Any, ActorRef]
//  )(
//    implicit system: ActorSystem,
//    materializer: Materializer
//  ): Flow[I, O, NotUsed] = {
//    val publisherProps = ProcessorPublisher.props[O]
//    val g = processorGraph[I, O](
//      name = name,
//      adapterPropsFromOutlet = ( outletRef: ActorRef ) => {
//        WatermarkProcessorAdapter.fixedProps( highWatermark, lowWatermark, outletRef )( workerPF )
//      },
//      outletProps = publisherProps
//    )
//
//    import StreamMonitor._
//    Flow.fromGraph( g ).watchFlow( Symbol(name) )
//  }
//
//  def fixedProcessorFlow[I, O: ClassTag](
//    name: String,
//    highWatermark: Int
//  )(
//    workerPF: PartialFunction[Any, ActorRef]
//  )(
//    implicit system: ActorSystem,
//    materializer: Materializer
//  ): Flow[I, O, NotUsed] = {
//    val publisherProps = ProcessorPublisher.props[O]
//    val g = processorGraph[I, O](
//      name = name,
//      adapterPropsFromOutlet = ( outletRef: ActorRef ) => {
//        WatermarkProcessorAdapter.fixedProps( highWatermark, outletRef )( workerPF )
//      },
//      outletProps = publisherProps
//    )
//
//    import StreamMonitor._
//    Flow.fromGraph( g ).watchFlow( Symbol(name) )
//  }
//
//  def elasticProcessorFlow[I, O: ClassTag](
//    name: String,
//    highWatermark: Int
//  )(
//    workerPF: PartialFunction[Any, ActorRef]
//  )(
//    implicit system: ActorSystem,
//    materializer: Materializer
//  ): Flow[I, O, NotUsed] = {
//    val publisherProps = ProcessorPublisher.props[O]
//    val g = processorGraph[I, O](
//      name = name,
//      adapterPropsFromOutlet = (outletRef: ActorRef) => {
//        WatermarkProcessorAdapter.elasticProps( highWatermark, outletRef )( workerPF )
//      },
//      outletProps = publisherProps
//    )
//
//    import StreamMonitor._
//    Flow.fromGraph( g ).watchFlow( Symbol(name) )
//  }
//
//  def elasticProcessorFlow[I, O: ClassTag](
//    name: String,
//    highWatermark: Int,
//    lowWatermark: Int
//  )(
//    workerPF: PartialFunction[Any, ActorRef]
//  )(
//    implicit system: ActorSystem,
//    materializer: Materializer
//  ): Flow[I, O, NotUsed] = {
//    val publisherProps = ProcessorPublisher.props[O]
//    val g = processorGraph[I, O](
//      name = name,
//      adapterPropsFromOutlet = (outletRef: ActorRef) => {
//        WatermarkProcessorAdapter.elasticProps( highWatermark, lowWatermark, outletRef )( workerPF )
//      },
//      outletProps = publisherProps
//    )
//
//    import StreamMonitor._
//    Flow.fromGraph( g ).watchFlow( Symbol(name) )
//  }
//
//  def processorGraph[I, O](
//    name: String,
//    outletProps: Props,
//    adapterPropsFromOutlet: ActorRef => Props
//  )(
//    implicit system: ActorSystem,
//    materializer: Materializer
//  ): Graph[FlowShape[I, O], NotUsed] = {
//    GraphDSL.create() { implicit b =>
//      val ( outletRef, outlet ) = {
//        Source.actorPublisher[O]( outletProps ).named( s"${name}-processor-outlet" )
//        .toMat( Sink.asPublisher(false) )( Keep.both )
//        .run()
//      }
//
//      val outletShape = b.add( Source fromPublisher[O] outlet )
//      val inletShape = b.add(
//        Sink.actorSubscriber[I]( adapterPropsFromOutlet( outletRef ) ).named( s"${name}-processor-inlet")
//      )
//
//      FlowShape( inletShape.in, outletShape.out )
//    }
//  }
//
//
//  def fixedProps( highWatermark: Int, lowWatermark: Int, outletRef: ActorRef )( workerPF: PartialFunction[Any, ActorRef] ): Props = {
//    val high = highWatermark
//    val low = lowWatermark
//    Props(
//      new WatermarkProcessorAdapter with TopologyProvider {
//        log.info( "[{}] setting watch on outlet:[{}]", self.path, outletRef.path )
//        context watch outletRef
//        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
//        override val highWatermark: Int = high
//        override val lowWatermark: Int = low
//        override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
//      }
//    )
//  }
//
//  def fixedProps( highWatermark: Int, outletRef: ActorRef )( workerPF: PartialFunction[Any, ActorRef] ): Props = {
//    val high = highWatermark
//    Props(
//      new WatermarkProcessorAdapter with TopologyProvider {
//        log.info( "[{}] setting watch on outlet:[{}]", self.path, outletRef.path )
//        context watch outletRef
//        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
//        override val highWatermark: Int = high
//        override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
//      }
//    )
//  }
//
//  def elasticProps(
//    highWatermark: Int,
//    lowWatermark: Int,
//    outletRef: ActorRef
//  )(
//    workerPF: PartialFunction[Any, ActorRef]
//  ): Props = {
//    val high = highWatermark
//    val low = lowWatermark
//    Props(
//      new WatermarkProcessorAdapter with TopologyProvider {
//        log.info( "[{}] setting watch on outlet:[{}]", self.path, outletRef.path )
//        context watch outletRef
//        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
//        override val highWatermark: Int = high
//        override val lowWatermark: Int = low
//        override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
//      }
//    )
//  }
//
//  def elasticProps( highWatermark: Int, outletRef: ActorRef )( workerPF: PartialFunction[Any, ActorRef] ): Props = {
//    val high = highWatermark
//    Props(
//      new WatermarkProcessorAdapter with TopologyProvider {
//        log.info( "[{}] setting watch on outlet:[{}]", self.path, outletRef.path )
//        context watch outletRef
//        override val workerFor: PartialFunction[Any, ActorRef] = workerPF
//        override val highWatermark: Int = high
//        override def outlet( implicit ctx: ActorContext ): ActorRef = outletRef
//      }
//    )
//  }
//
//
//  trait TopologyProvider {
//    def workerFor: PartialFunction[Any, ActorRef]
//    def outlet( implicit context: ActorContext ): ActorRef
//    def highWatermark: Int
//    def lowWatermark: Int = highWatermark / 2
//  }
//
//
//  case class DetectionJob( destination: ActorRef, startNanos: Long = System.nanoTime() )
//
//
//  final case class DeadWorkerError private[WatermarkProcessorAdapter]( deadWorker: ActorRef )
//  extends IllegalStateException( s"Flow's Watermark Processor notified of worker death: [${deadWorker}]" )
//}
//
//class WatermarkProcessorAdapter extends ActorSubscriber with InstrumentedActor with ActorLogging {
//  outer: WatermarkProcessorAdapter.TopologyProvider =>
//
//  val submissionMeter: Meter = metrics meter "submission"
//  val publishMeter: Meter = metrics meter "publish"
//
//
//  override protected def requestStrategy: RequestStrategy = {
//    new WatermarkRequestStrategy( highWatermark = outer.highWatermark, lowWatermark = outer.lowWatermark )
//  }
//
//
//  override def receive: Receive = LoggingReceive {
//    around( withSubscriber(outer.outlet) orElse maintenance /* see publish below: orElse publish(outer.outlet) */ )
//  }
//
//  def withSubscriber( outlet: ActorRef ): Receive = {
//    case next @ ActorSubscriberMessage.OnNext( message ) if outer.workerFor.isDefinedAt( message ) => {
//      submissionMeter.mark()
//      val worker = outer workerFor message
//      context watch worker
//      // since the watermark strategy does not need to track outstanding in flight, directly connect worker to outlet
//      worker.tell( message, sender = outlet )
//    }
//
//    case ActorSubscriberMessage.OnComplete => outlet ! ActorSubscriberMessage.OnComplete
//
//    case onError: ActorSubscriberMessage.OnError => outlet ! onError
//  }
//
//  val maintenance: Receive = {
//    case Terminated( deadOutlet ) if deadOutlet == outer.outlet => {
//      log.error( "[{}] notified of dead outlet:[{}] - stopping processor", self.path, outer.outlet.path )
//      context stop self
//    }
//
//    case Terminated( deadWorker ) => {
//      log.error( "[{}] notified of dead worker:[{}]", self.path, deadWorker.path )
//      //todo is this response appropriate for spotlight and generally?
//      outer.outlet ! ActorSubscriberMessage.OnError( WatermarkProcessorAdapter.DeadWorkerError(deadWorker) )
//    }
//  }
//
//  // since worker is directly connected to outlet, no need for publish
////  def publish( outlet: ActorRef ): Receive = {
////    case message => {
////      publishMeter.mark()
////      outlet ! message
////    }
////  }
//}
