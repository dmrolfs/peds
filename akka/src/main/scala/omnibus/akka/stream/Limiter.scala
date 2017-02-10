package omnibus.akka.stream

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import akka.NotUsed
import akka.actor.{Actor, ActorRef, Props, ActorLogging, Cancellable}
import akka.event.LoggingReceive
import akka.stream.scaladsl.Flow
import com.codahale.metrics.{Metric, MetricFilter}
import nl.grons.metrics.scala.MetricName
import omnibus.akka.metrics.InstrumentedActor


/**
  * Adapted from Akka Streams Cookbook Limiter class
  * Created by rolfsd on 1/2/16.
  */
object Limiter {
  def props( maxAvailableTokens: Int, tokenRefreshPeriod: FiniteDuration, tokenRefreshAmount: Int ): Props = {
    Props( new Limiter(maxAvailableTokens, tokenRefreshPeriod, tokenRefreshAmount) with ConfigurationProvider )
  }

  def limitGlobal[T](
    limiter: ActorRef,
    maxAllowedWait: FiniteDuration
  )(
    implicit ec: ExecutionContext
  ): Flow[T, T, NotUsed] = {
    import akka.pattern.ask
    import akka.util.Timeout

    Flow[T].mapAsync( 4 ){ (element: T) =>
      implicit val triggerTimeout = Timeout( maxAllowedWait )
      val limiterTriggerFuture = limiter ? Limiter.WantToPass
      limiterTriggerFuture map { (_) => element }
    }
  }


  sealed trait LimiterProtocol
  case object WantToPass extends LimiterProtocol
  case object MayPass extends LimiterProtocol
  case object ReplenishTokens extends LimiterProtocol


  trait ConfigurationProvider {
    def metricNameRoot: String = ""
  }
}

class Limiter(
  val maxAvailableTokens: Int,
  val tokenRefreshPeriod: FiniteDuration,
  val tokenRefreshAmount: Int
) extends Actor with InstrumentedActor with ActorLogging { outer: Limiter.ConfigurationProvider =>
  import Limiter._
  import context.dispatcher
  import akka.actor.Status

  override lazy val metricBaseName: MetricName = MetricName( outer.metricNameRoot + classOf[Limiter].getName )
  val availableMetricName: String = "available"
  val waitingMetricName: String = "waiting"
  val passingMeter = metrics meter "passing"

  override def preStart(): Unit = {
    super.preStart()
    initializeMetrics()
  }
  
  def initializeMetrics(): Unit = {
    stripLingeringMetrics()
    metrics.gauge( waitingMetricName ){ waitQueue.size }
    metrics.gauge( availableMetricName ) { permitTokens }
  }

  def stripLingeringMetrics(): Unit = {
    metrics.registry.removeMatching(
      new MetricFilter {
        override def matches( name: String, metric: Metric ): Boolean = {
          name.contains( classOf[Limiter].getName ) && ( name.contains(availableMetricName) || name.contains(waitingMetricName) )
        }
      }
    )
  }
  

  private var waitQueue: immutable.Queue[ActorRef] = immutable.Queue.empty[ActorRef]
  private var permitTokens: Int = maxAvailableTokens
  private val replenishTimer: Cancellable = context.system.scheduler.schedule(
    initialDelay = tokenRefreshPeriod,
    interval = tokenRefreshPeriod,
    receiver = self,
    ReplenishTokens
  )

  override def receive: Receive = around( open )

  val open: Receive = LoggingReceive {
    case ReplenishTokens => permitTokens = math.min( permitTokens + tokenRefreshAmount, maxAvailableTokens )

    case WantToPass => {
      passingMeter.mark()
      permitTokens -= 1
      sender() ! MayPass
      if ( permitTokens == 0 ) context become around( closed )
    }
  }

  val closed: Receive = LoggingReceive {
    case ReplenishTokens => {
      permitTokens = math.min( permitTokens + tokenRefreshAmount, maxAvailableTokens )
      releaseWaiting()
    }

    case WantToPass => {
      waitQueue = waitQueue enqueue sender()
    }
  }

  private def releaseWaiting(): Unit = {
    val ( toBeReleased, remainingQueue ) = waitQueue splitAt permitTokens
    waitQueue = remainingQueue
    permitTokens -= toBeReleased.size
    toBeReleased foreach { _ ! MayPass }
    passingMeter.mark( toBeReleased.size )
    if ( permitTokens > 0 ) context become around( open )
  }

  override def postStop(): Unit = {
    replenishTimer.cancel()
    waitQueue foreach { _ ! Status.Failure( new IllegalStateException("limiter stopped") ) }
  }
}
