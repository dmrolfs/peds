package peds.akka.stream

import akka.actor.{ActorLogging, ActorRef, Props, Terminated}
import akka.event.LoggingReceive
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, RequestStrategy, WatermarkRequestStrategy}
import nl.grons.metrics.scala.{Meter, MetricName}
import peds.akka.metrics.InstrumentedActor


/**
  * Created by rolfsd on 4/19/16.
  */
object StreamEgress {
  def props( subscriber: ActorRef, high: Int, low: Option[Int] = None ): Props = {
    Props(
      new StreamEgress( subscriber ) with WatermarkProvider {
        override def highWatermark: Int = high
      }
    )
  }


  trait WatermarkProvider {
    def highWatermark: Int
    def lowWatermark: Int = highWatermark / 2
  }
}

class StreamEgress( subscriber: ActorRef ) extends ActorSubscriber with InstrumentedActor with ActorLogging {
  outer: StreamEgress.WatermarkProvider =>

  context watch subscriber

  override lazy val metricBaseName: MetricName = MetricName( classOf[StreamEgress] )
  val egressMeter: Meter = metrics meter "egress"

  override protected def requestStrategy: RequestStrategy = {
    new WatermarkRequestStrategy( highWatermark = outer.highWatermark, lowWatermark = outer.lowWatermark )
  }

  override def receive: Receive = LoggingReceive { around( egress orElse maintenance ) }

  val egress: Receive = {
    case next @ ActorSubscriberMessage.OnNext( message ) => {
      egressMeter.mark()
      log.debug( "StreamEgress: Sending [{}] message:[{}]", subscriber, message )
      subscriber ! message
    }

    case ActorSubscriberMessage.OnComplete => subscriber ! ActorSubscriberMessage.OnComplete
    case onError: ActorSubscriberMessage.OnError => subscriber ! onError
  }

  val maintenance: Receive = {
    case Terminated( deadActor ) => {
      log.warning( "stream-egress [{}] notified of death of subscriber:[{}] -- stopping", self.path, subscriber.path )
      context stop self
    }
  }
}
