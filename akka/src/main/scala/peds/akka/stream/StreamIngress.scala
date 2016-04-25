package peds.akka.stream

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.actor.{ActorLogging, Props}
import akka.event.LoggingReceive
import akka.stream.actor._
import nl.grons.metrics.scala.{Meter, MetricName}
import peds.akka.metrics.InstrumentedActor


/**
  * Created by rolfsd on 4/19/16.
  */
object StreamIngress {
  def props[T: ClassTag]: Props = Props( new StreamIngress[T] )

  trait IngressProtocol
  case object CompleteAndStop extends IngressProtocol
}

class StreamIngress[T: ClassTag] extends ActorPublisher[T] with InstrumentedActor with ActorLogging {
  override lazy val metricBaseName: MetricName = MetricName( classOf[StreamIngress[T]] )
  val ingressdMeter: Meter = metrics meter "ingress"

  val tTag: ClassTag[T] = implicitly[ClassTag[T]]
  log.debug( "StreamIngress tTag:[{}]", tTag )
  var buffer: Vector[T] = Vector.empty[T]


  override def subscriptionTimeout: Duration = 5.seconds

  override def receive: Receive = LoggingReceive{ around( ingress ) }

  val ingress: Receive = {
    case tTag( message ) if isActive => {
      log.debug( "StreamIngress: buffer-size:[{}] totalDemand:[{}] Recd-Message:[{}]", buffer.size, totalDemand, message )
      if ( buffer.isEmpty && totalDemand > 0 ) onNext( message )
      else {
        buffer :+= message
        deliverBuffer()
      }
    }

    case ActorPublisherMessage.Request( n ) => deliverBuffer()

    case ActorPublisherMessage.Cancel => context stop self

    case StreamIngress.CompleteAndStop => {
      deliverBuffer()
      onCompleteThenStop()
    }

    case m => log.error( "StreamIngress[active={}]: received UNKNOWN message:[{}]", isActive, m )
  }

  @tailrec final def deliverBuffer(): Unit = {
    if ( isActive && totalDemand > 0 ) {
      if ( totalDemand <= Int.MaxValue ) {
        val (use, keep) = buffer splitAt totalDemand.toInt
        use foreach onNext
        buffer = keep
      } else {
        val (use, keep) = buffer splitAt Int.MaxValue
        use foreach onNext
        buffer = keep
        deliverBuffer()
      }
    }
  }
}
