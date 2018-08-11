package omnibus.akka.pattern

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.FiniteDuration
import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  ActorRefFactory,
  Cancellable,
  Props,
  ReceiveTimeout
}
import akka.event.LoggingReceive
import akka.util.Timeout
import cats.syntax.validated._
import cats.syntax.either._
import nl.grons.metrics.scala.{ Meter, MetricName, Timer }
import omnibus.akka.metrics.InstrumentedActor
import omnibus.core.{ AllErrorsOr, AllIssuesOr }
import omnibus.identifier.ShortUUID

/**
  * Created by rolfsd on 4/12/16.
  */
object ResponseCollector {

  def apply[T](
    label: String,
    tracker: Tracker[T],
    reducer: Reducer[T] = new IdentityReducer[T]
  )(
    matcher: PartialFunction[Any, T]
  )(
    implicit timeout: Timeout,
    factory: ActorRefFactory
  ): ( Future[Result[T]], ActorRef ) = {
    val result = Promise[Result[T]]()
    val ref = factory.actorOf(
      props( label, tracker, result, reducer )( matcher ),
      s"${label}-collector-${ShortUUID()}"
    )
    ( result.future, ref )
  }

  def props[T](
    label: String,
    initialTracker: Tracker[T],
    result: Promise[Result[T]],
    reducer: Reducer[T] = new IdentityReducer[T]
  )(
    matcher: PartialFunction[Any, T]
  )(
    implicit timeout: Timeout
  ): Props = {
    Props(
      new ResponseCollector[T]( label, timeout.duration, initialTracker, result, matcher, reducer )
      with ConfigurationProvider {
        override val warningsBeforeTimeout: Int = 0
      }
    )
  }

  abstract class Tracker[T] {
    def addResponse( response: T ): Tracker[T]
    def isDone: Boolean
    def remainingResponsesAtTimeout: String
  }

  object Countdown {

    def apply[T]( expectedMessages: Int ): AllIssuesOr[Tracker[T]] = {
      checkExpectedMessages( expectedMessages ) map { em =>
        SimpleCountdown[T]( expectedMessages = em )
      }
    }

    def checkExpectedMessages( expected: Int ): AllIssuesOr[Int] = {
      if (expected >= 0) expected.validNel
      else {
        new IllegalArgumentException(
          s"expected messages [${expected}] must be greater than or equal to 0"
        ).invalidNel
      }
    }

    final case class SimpleCountdown[T] private[ResponseCollector] ( expectedMessages: Int )
        extends Tracker[T] {
      override def addResponse( response: T ): Tracker[T] =
        copy( expectedMessages = (expectedMessages - 1) max 0 )
      override def isDone: Boolean = expectedMessages == 0
      override def remainingResponsesAtTimeout: String = expectedMessages.toString
    }
  }

  object MatchIds {
    def toSet[I]: I => Set[I] = (id: I) => { Set( id ) }

    def fromPlural[M, I]( expectedIds: Set[I], toIds: M => Set[I] ): AllIssuesOr[Tracker[M]] = {
      SimpleMatchIds( expectedIds, toIds ).validNel
    }

    def fromSingular[M, I]( expectedIds: Set[I], toId: M => I ): AllIssuesOr[Tracker[M]] = {
      SimpleMatchIds( expectedIds, toSet compose toId ).validNel
    }

    final case class SimpleMatchIds[M, I] private[ResponseCollector] (
      expectedIds: Set[I],
      toIds: M => Set[I]
    ) extends Tracker[M] {
      override def addResponse( response: M ): Tracker[M] =
        copy( expectedIds = expectedIds -- toIds( response ) )
      override def isDone: Boolean = expectedIds.isEmpty
      override def remainingResponsesAtTimeout: String = expectedIds.mkString( "[", ", ", "]" )
    }
  }

  abstract class Reducer[T] extends Function1[Iterable[T], AllErrorsOr[Iterable[T]]]

  class IdentityReducer[T] extends Reducer[T] {
    override def apply( responses: Iterable[T] ): AllErrorsOr[Iterable[T]] = { responses.asRight }
  }

  sealed trait CollectorProtocol
  case class Result[T]( payload: Iterable[T], state: ResultState ) extends CollectorProtocol

  sealed trait ResultState

  case object Full extends ResultState {
    override def toString: String = "full"
  }

  case object Partial extends ResultState {
    override def toString: String = "partial"
  }

  trait ConfigurationProvider {
    def warningsBeforeTimeout: Int

    def warningBudget( totalLatencyBudget: FiniteDuration ): FiniteDuration = {
      if (warningsBeforeTimeout == 0) totalLatencyBudget
      else totalLatencyBudget / warningsBeforeTimeout.toLong
    }
  }
}

class ResponseCollector[T](
  label: String,
  timeout: FiniteDuration,
  initialTracker: ResponseCollector.Tracker[T],
  result: Promise[ResponseCollector.Result[T]],
  matcher: PartialFunction[Any, T],
  reducer: ResponseCollector.Reducer[T] = new ResponseCollector.IdentityReducer[T]
) extends Actor
    with InstrumentedActor
    with ActorLogging {
  outer: ResponseCollector.ConfigurationProvider =>

  import ResponseCollector._
  import context.dispatcher

  override lazy val metricBaseName: MetricName = MetricName( classOf[ResponseCollector[T]] )
  lazy val conclusionsMeter: Meter = metrics.meter( "quorum." + label, ".conclusions" )
  lazy val warningsMeter: Meter = metrics.meter( "quorum." + label, ".warnings" )
  lazy val timeoutsMeter: Meter = metrics.meter( "quorum." + label, ".timeout" )
  lazy val quorumTimer: Timer = metrics.timer( "quorum", label )
  val originNanos: Long = System.nanoTime()

  var pendingWhistle: Option[Cancellable] = None
  scheduleWhistle()

  def scheduleWhistle( untilWhistle: FiniteDuration = outer.warningBudget( timeout ) ): Unit = {
    cancelWhistle()
    pendingWhistle = Some(
      context.system.scheduler.scheduleOnce( untilWhistle, self, ReceiveTimeout )
    )
  }

  def cancelWhistle(): Unit = {
    pendingWhistle foreach { _.cancel() }
    pendingWhistle = None
  }

  override def postStop(): Unit = cancelWhistle()

  override def receive: Receive = LoggingReceive {
    around( ready( Vector.empty[T], initialTracker ) )
  }

  private def ready(
    responses: Vector[T],
    tracker: Tracker[T],
    warningsRemaining: Int = outer.warningsBeforeTimeout
  ): Receive = {
    case m if matcher isDefinedAt m => {
      val response = matcher( m )
      val nextResponses = responses :+ response
      val nextTracker = tracker addResponse response

      if (nextTracker.isDone) publishAndStop( nextResponses, Full )
      else
        context become LoggingReceive {
          around( ready( nextResponses, nextTracker, warningsRemaining ) )
        }
    }

    case ReceiveTimeout if warningsRemaining > 0 => {
      val nextWarningsRemaining = warningsRemaining - 1
      warningsMeter.mark()
      log.warning(
        "quorum not reached for [{}] warnings-left:[{}] remaining:[{}]",
        label,
        warningsRemaining,
        tracker.remainingResponsesAtTimeout
      )

      scheduleWhistle()
      context become LoggingReceive { around( ready( responses, tracker, nextWarningsRemaining ) ) }
    }

    case ReceiveTimeout => {
      if (tracker.isDone) publishAndStop( responses, Full )
      else {
        timeoutsMeter.mark()
        log.warning(
          "Response collecting timed out for [{}] with [{}] responses remaining; publishing partial response",
          label,
          tracker.remainingResponsesAtTimeout
        )
        publishAndStop( responses, Partial )
      }

    }
  }

  private def publishAndStop( responses: Vector[T], state: ResultState ): Unit = {
    quorumTimer.update( System.nanoTime() - originNanos, scala.concurrent.duration.NANOSECONDS )
    if (state == Full) conclusionsMeter.mark()

    reducer( responses ) match {
      case Right( rs ) => result.success( Result( payload = rs, state ) )
      case Left( exs ) => {
        exs map { ex =>
          log.error( "failed to reduce responses for [{}] due to: {}", label, ex )
        }
        // not throwing since stopping anyway
      }
    }

    context stop self
  }
}
