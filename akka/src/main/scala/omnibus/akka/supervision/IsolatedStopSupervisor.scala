package omnibus.akka.supervision

import scala.concurrent.duration.Duration
import akka.actor.{ ActorInitializationException, ActorKilledException, SupervisorStrategy }
import akka.actor.SupervisorStrategy.{ Escalate, Stop }

abstract class IsolatedStopSupervisor(
  maxNrRetries: Int = -1,
  withinTimeRange: Duration = Duration.Inf
) extends IsolatedLifeCycleSupervisor {
  self: SupervisionStrategyFactory =>

  override val supervisorStrategy: SupervisorStrategy =
    makeStrategy( maxNrRetries, withinTimeRange ) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException         => Stop
      case _: Exception                    => Stop
      case _                               => Escalate
    }
}
