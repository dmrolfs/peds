package peds.akka.supervision

import scala.concurrent.duration.Duration
import akka.actor.{ SupervisorStrategy, ActorInitializationException, ActorKilledException }
import akka.actor.SupervisorStrategy.{ Escalate, Resume, Stop }


abstract class IsolatedResumeSupervisor( 
  maxNrRetries: Int = -1, 
  withinTimeRange: Duration = Duration.Inf 
) extends IsolatedLifeCycleSupervisor {
  self: SupervisionStrategyFactory =>

  override val supervisorStrategy: SupervisorStrategy = makeStrategy( maxNrRetries, withinTimeRange ) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}
