package omnibus.akka.stream

import scala.concurrent.duration.FiniteDuration
import akka.actor.{ ActorContext, ActorLogging, ActorPath, ActorRef, Props, Terminated }
import akka.contrib.pattern.ReliableProxy
import akka.event.LoggingReceive
import akka.stream.actor.{
  ActorSubscriber,
  ActorSubscriberMessage,
  RequestStrategy,
  WatermarkRequestStrategy
}
import nl.grons.metrics.scala.{ Meter, MetricName }
import omnibus.akka.metrics.InstrumentedActor
import com.github.ghik.silencer.silent

/**
  * Created by rolfsd on 4/19/16.
  */
object StreamEgress {

  def props( subscriberMagnet: SubscriberMagnet, high: Int ): Props = {
    Props( new Default( subscriberMagnet, high ) )
  }

  sealed trait SubscriberMagnet {
    def apply(): ActorRef
  }

  object SubscriberMagnet {
    import scala.language.implicitConversions

    implicit def fromRef( ref: ActorRef ): SubscriberMagnet = new SubscriberMagnet {
      override def apply(): ActorRef = ref
    }

    @silent implicit def fromPathForReliableFullOptions(
      tuple: ( ActorPath, FiniteDuration, Option[FiniteDuration], Option[Int] )
    )(
      implicit context: ActorContext
    ): SubscriberMagnet = {
      new SubscriberMagnet {
        val (
          path: ActorPath,
          retryAfter: FiniteDuration,
          reconnectAfter: Option[FiniteDuration],
          maxConnectAttempts: Option[Int]
        ) = tuple

        lazy val subscriber: ActorRef = context.actorOf(
          ReliableProxy.props( path, retryAfter, reconnectAfter, maxConnectAttempts )
        )

        override def apply(): ActorRef = subscriber
      }
    }

    @silent implicit def fromPathForReliableNoReconnectionLimit(
      tuple: ( ActorPath, FiniteDuration, FiniteDuration )
    )(
      implicit context: ActorContext
    ): SubscriberMagnet = {
      new SubscriberMagnet {
        val ( path: ActorPath, retryAfter: FiniteDuration, reconnectAfter: FiniteDuration ) = tuple
        lazy val subscriber: ActorRef =
          context.actorOf( ReliableProxy.props( path, retryAfter, Option( reconnectAfter ), None ) )

        override def apply(): ActorRef = subscriber
      }
    }

    @silent implicit def fromPathForReliableNoReconnections(
      tuple: ( ActorPath, FiniteDuration )
    )(
      implicit context: ActorContext
    ): SubscriberMagnet = {
      new SubscriberMagnet {
        val ( path: ActorPath, retryAfter: FiniteDuration ) = tuple
        lazy val subscriber: ActorRef =
          context.actorOf( ReliableProxy.props( path, retryAfter, None, None ) )

        override def apply(): ActorRef = subscriber
      }
    }
  }

  private class Default(
    subscriberMagnet: SubscriberMagnet,
    override val highWatermark: Int
  ) extends StreamEgress( subscriberMagnet )
      with WatermarkProvider

  trait WatermarkProvider {
    def highWatermark: Int
    def lowWatermark: Int = highWatermark / 2
  }
}

import StreamEgress.SubscriberMagnet

@silent
class StreamEgress( subscriberMagnet: SubscriberMagnet )
    extends ActorSubscriber
    with InstrumentedActor
    with ActorLogging {
  outer: StreamEgress.WatermarkProvider =>

  val subscriber = subscriberMagnet()
  context watch subscriber

  override lazy val metricBaseName: MetricName = MetricName( classOf[StreamEgress] )
  val egressMeter: Meter = metrics meter "egress"

  override protected def requestStrategy: RequestStrategy = {
    new WatermarkRequestStrategy(
      highWatermark = outer.highWatermark,
      lowWatermark = outer.lowWatermark
    )
  }

  override def receive: Receive = LoggingReceive { around( egress orElse maintenance ) }

  val egress: Receive = {
    case ActorSubscriberMessage.OnNext( message ) => {
      egressMeter.mark()
      log.debug( "StreamEgress[{}]: Sending [{}] message:[{}]", this.##, subscriber, message )
      subscriber ! message
    }

    case ActorSubscriberMessage.OnComplete => {
      log.debug(
        "StreamEgress[{}][{}] received OnComplete from [{}] forwarding to subscriber:[{}]",
        self.path,
        this.##,
        sender().path,
        subscriber.path
      )

      subscriber ! ActorSubscriberMessage.OnComplete
    }
    case onError @ ActorSubscriberMessage.OnError( cause ) => {
      log.error(
        cause,
        "StreamEgress[{}][{}] received OnError from [{}] forwarding to subscriber:[{}]",
        self.path,
        this.##,
        sender().path,
        subscriber.path
      )

      subscriber ! onError
    }
  }

  val maintenance: Receive = {
    case _: Terminated => {
      log.warning(
        "StreamEgress[{}][{}] notified of death of subscriber:[{}] -- stopping",
        self.path,
        this.##,
        subscriber.path
      )

      context stop self
    }
  }
}
