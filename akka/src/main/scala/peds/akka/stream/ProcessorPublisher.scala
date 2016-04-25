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

class ProcessorPublisher[O: ClassTag]
extends Actor
with ActorPublisher[O]
with InstrumentedActor
with ActorLogging {
  val oTag = implicitly[ClassTag[O]]
  log.debug( "Publisher oTag = {}", oTag )
  var buffer: Vector[O] = Vector.empty[O]


  override lazy val metricBaseName: MetricName = MetricName( classOf[ProcessorPublisher[O]] )

  override val subscriptionTimeout: Duration = 5.seconds

  override def receive: Receive = LoggingReceive { around( publish ) }

  val publish: Receive = {
    case oTag( message ) if isActive => {
      log.debug( "Publisher received message: [{}][{}]", oTag.runtimeClass.getName, message )
      if ( buffer.isEmpty && totalDemand > 0 ) {
        log.debug(
          "ProcessorPublisher: there is demand [{}:{}] so short-circuiting buffer[{}] to onNext message: [{}]",
          totalDemand,
          isActive,
          buffer.size,
          message
        )
        onNext( message )
      } else {
        buffer :+= message
        log.debug( "ProcessorPublisher: buffered [new-size={}] result: [{}]", buffer.size, message )
        deliverBuffer()
      }
    }

    case oTag( message ) => log.info( "ignoring received message [{}]: [{}] while not active", oTag, message )

    case ActorSubscriberMessage.OnComplete => {
      deliverBuffer()
      onComplete()
    }

    case ActorSubscriberMessage.OnError( cause ) => onError( cause )

    case _: ActorPublisherMessage.Request => {
      log.debug( "ProcessorPublisher: downstream request received: totalDemand=[{}]", totalDemand )
      deliverBuffer()
    }

    case ActorPublisherMessage.Cancel => {
      log.info( "cancelling detection analysis - leaving buffered:[{}]", buffer.size )
      context stop self
    }

    case m => log.error( "received not tag[{}] but UNKNOWN message: [{}]", oTag, m )
  }


  @tailrec final def deliverBuffer(): Unit = {
    if ( isActive && totalDemand > 0 ) {
      if ( totalDemand <= Int.MaxValue ) {
        log.debug( "ProcessorPublisher: delivering {} of {} demand", totalDemand.toInt, totalDemand.toInt )
        val (use, keep) = buffer splitAt totalDemand.toInt
        use foreach onNext
        buffer = keep
      } else {
        log.debug( "ProcessorPublisher: delivering {} of {} demand", Int.MaxValue, totalDemand.toInt )
        val (use, keep) = buffer splitAt Int.MaxValue
        use foreach onNext
        buffer = keep
        deliverBuffer()
      }
    }
  }
}
