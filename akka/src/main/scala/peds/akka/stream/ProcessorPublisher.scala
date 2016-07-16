package peds.akka.stream

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.actor._
import akka.event.LoggingReceive
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage, ActorSubscriberMessage}
import nl.grons.metrics.scala.MetricName
import peds.akka.metrics.InstrumentedActor


/**
  * Created by rolfsd on 3/30/16.
  */
object ProcessorPublisher {
  def props[O: ClassTag]: Props = Props( new ProcessorPublisher[O] )
}

class ProcessorPublisher[O: ClassTag] extends ActorPublisher[O] with InstrumentedActor with ActorLogging {
  override lazy val metricBaseName: MetricName = MetricName( classOf[ProcessorPublisher[O]] )

  override val subscriptionTimeout: Duration = 5.seconds


  val oTag = implicitly[ClassTag[O]]
  log.debug( "Publisher oTag = {}", oTag )
  var buffer: Vector[O] = Vector.empty[O]


  override def receive: Receive = LoggingReceive { around( publish ) }

  val publish: Receive = {
    case oTag( message ) if isActive => {
      log.debug( "Publisher received message: [{}][{}]", oTag.runtimeClass.getName, message )
      if ( buffer.isEmpty && totalDemand > 0 ) {
        log.debug(
          "ProcessorPublisher[{}]: there is demand [{}] so short-circuiting buffer[{}] to onNext message: [{}]",
          (self.path, this##),
          (totalDemand,isActive),
          buffer.size,
          message
        )
        onNext( message )
      } else {
        buffer :+= message
        log.debug( "ProcessorPublisher[{}][{}]: buffered [new-size={}] result: [{}]", self.path, this.##, buffer.size, message )
        deliverBuffer()
      }
    }

    case oTag( message ) => {
      log.info(
        "ProcessorPublisher[{}][{}] ignoring received message [{}]: [{}] while not active",
        self.path,
        this.##,
        oTag,
        message
      )
    }

    case ActorSubscriberMessage.OnComplete => {
      log.info( "ProcessorPublisher[{}][{}] received OnComplete from [{}]", self.path, this.##, sender().path )
      deliverBuffer()
      onComplete()
    }

    case ActorSubscriberMessage.OnError( cause ) => {
      log.error( cause, "ProcessorPublisher[{}][{}] received OnError from [{}]", self.path, this.##, sender().path )
      onError( cause )
    }

    case _: ActorPublisherMessage.Request => {
      log.debug( "ProcessorPublisher[{}][{}]: downstream request received: totalDemand=[{}]", self.path, this.##, totalDemand )
      deliverBuffer()
    }

    case ActorPublisherMessage.Cancel => {
      log.info(
        "ProcessorPublisher[{}][{}] received Cancel from [{}] -- stopping leaving buffered:[{}]",
        self.path,
        this.##,
        sender().path,
        buffer.size
      )
      context stop self
    }

    case m => {
      log.error( "ProcessorPublisher[{}][{}] received not tag[{}] but UNKNOWN message: [{}]", self.path, this.##, oTag, m )
    }
  }


  @tailrec final def deliverBuffer(): Unit = {
    if ( isActive && totalDemand > 0 ) {
      if ( totalDemand <= Int.MaxValue ) {
        log.debug(
          "ProcessorPublisher[{}][{}]: delivering {} of {} demand",
          self.path,
          this.##,
          totalDemand.toInt,
          totalDemand.toInt
        )
        val (use, keep) = buffer splitAt totalDemand.toInt
        use foreach onNext
        buffer = keep
      } else {
        log.debug(
          "ProcessorPublisher[{}][{}]: delivering {} of {} demand",
          self.path,
          this.##,
          Int.MaxValue,
          totalDemand.toInt
        )
        val (use, keep) = buffer splitAt Int.MaxValue
        use foreach onNext
        buffer = keep
        deliverBuffer()
      }
    }
  }
}
