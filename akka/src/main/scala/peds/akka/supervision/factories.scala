package peds.akka.supervision

import scala.concurrent.duration.Duration
import akka.actor.{ SupervisorStrategy, OneForOneStrategy, AllForOneStrategy }
import akka.actor.SupervisorStrategy.Decider


trait SupervisionStrategyFactory {
  def makeStrategy( maxNrRetries: Int, withinTimeRange: Duration )( decider: Decider ): SupervisorStrategy
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  override def makeStrategy( maxNrRetries: Int, withinTimeRange: Duration )( decider: Decider ): SupervisorStrategy = {
    OneForOneStrategy( maxNrRetries, withinTimeRange )( decider )
  }
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  override def makeStrategy( maxNrRetries: Int, withinTimeRange: Duration )( decider: Decider ): SupervisorStrategy = {
    AllForOneStrategy( maxNrRetries, withinTimeRange )( decider )
  }
}
