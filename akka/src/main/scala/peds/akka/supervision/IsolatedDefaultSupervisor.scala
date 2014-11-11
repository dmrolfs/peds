package peds.akka.supervision

import scala.concurrent.duration.Duration
import akka.actor.{ SupervisorStrategy, ActorInitializationException, ActorKilledException }
import akka.actor.SupervisorStrategy.{ Stop, Escalate }


abstract class IsolatedDefaultSupervisor( 
  maxNrRetries: Int = -1, 
  withinTimeRange: Duration = Duration.Inf 
) extends IsolatedLifeCycleSupervisor {
  self: SupervisionStrategyFactory =>

  override val supervisorStrategy: SupervisorStrategy = {
    makeStrategy( maxNrRetries, withinTimeRange )( SupervisorStrategy.defaultDecider )
  }
}
