package omnibus.akka.stream

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.actor._
import akka.event.LoggingReceive
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage, ActorSubscriberMessage}
import nl.grons.metrics.scala.MetricName
import omnibus.akka.metrics.InstrumentedActor


/**
  * Created by rolfsd on 3/30/16.
  */
object CommonActorPublisher {
  def props[O: ClassTag]( subscriptionTimeout: Duration = Duration.Inf ): Props = Props( new Default[O]( subscriptionTimeout ) )

  private class Default[O: ClassTag]( override val subscriptionTimeoutDuration: Duration )
  extends CommonActorPublisher[O] with ConfigurationProvider


  trait ConfigurationProvider {
    def subscriptionTimeoutDuration: Duration
  }
}

class CommonActorPublisher[O: ClassTag] extends ActorPublisher[O] with InstrumentedActor with ActorLogging {
  outer: CommonActorPublisher.ConfigurationProvider =>

  override val subscriptionTimeout: Duration = outer.subscriptionTimeoutDuration

  override lazy val metricBaseName: MetricName = MetricName( classOf[CommonActorPublisher[O]] )


  val oTag = implicitly[ClassTag[O]]
  log.debug( "CommonActorPublisher oTag = {}", oTag )
  var buffer: Vector[O] = Vector.empty[O]


  override def receive: Receive = LoggingReceive { around( publish ) }

  val publish: Receive = {
    case oTag( message ) if isActive => {
      log.debug( "CommonActorPublisher received message: [{}][{}]", oTag.runtimeClass.getName, message )
      if ( buffer.isEmpty && totalDemand > 0 ) {
        log.debug(
          "CommonActorPublisher[{}]: there is demand [{}] so short-circuiting buffer[{}] to onNext message: [{}]",
          (self.path, this##),
          (totalDemand,isActive),
          buffer.size,
          message
        )
        onNext( message )
      } else {
        buffer :+= message
        log.debug( "CommonActorPublisher[{}][{}]: buffered [new-size={}] result: [{}]", self.path, this.##, buffer.size, message )
        deliverBuffer()
      }
    }

    case oTag( message ) => {
      log.info(
        "CommonActorPublisher[{}][{}] ignoring received message [{}]: [{}] while not active",
        self.path,
        this.##,
        oTag,
        message
      )
    }

    case ActorSubscriberMessage.OnComplete => {
      log.info( "CommonActorPublisher[{}][{}] received OnComplete from [{}]", self.path, this.##, sender().path )
      deliverBuffer()
      onComplete()
    }

    case ActorSubscriberMessage.OnError( cause ) => {
      log.error( cause, "CommonActorPublisher[{}][{}] received OnError from [{}]", self.path, this.##, sender().path )
      onError( cause )
    }

    case _: ActorPublisherMessage.Request => {
      log.debug( "CommonActorPublisher[{}][{}]: downstream request received: totalDemand=[{}]", self.path, this.##, totalDemand )
      deliverBuffer()
    }

    case ActorPublisherMessage.Cancel => {
      log.info(
        "CommonActorPublisher[{}][{}] received Cancel from [{}] -- stopping leaving buffered:[{}]",
        self.path,
        this.##,
        sender().path,
        buffer.size
      )
      context stop self
    }

    case ActorPublisherMessage.SubscriptionTimeoutExceeded => {
      log.warning( "subscription timeout exceeded. stopping common publisher at {}", self.path )
      context stop self
    }

    case m => {
      log.error( "CommonActorPublisher[{}][{}] received not tag[{}] but UNKNOWN message: [{}]", self.path, this.##, oTag, m )
    }
  }


  @tailrec final def deliverBuffer(): Unit = {
    if ( isActive && totalDemand > 0 ) {
      if ( totalDemand <= Int.MaxValue ) {
        log.debug(
          "CommonActorPublisher[{}][{}]: delivering {} of {} demand",
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
          "CommonActorPublisher[{}][{}]: delivering {} of {} demand",
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
