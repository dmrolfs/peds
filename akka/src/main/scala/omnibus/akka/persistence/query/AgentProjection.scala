package omnibus.akka.persistence

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.agent.Agent
import akka.persistence.query.scaladsl.Que
import com.typesafe.scalalogging.LazyLogging


/**
  * Created by rolfsd on 2/15/17.
  */
class AgentProjection[T]( zero: T, queryJournal: QueryJournal )( implicit system: ActorSystem, ec: ExecutionContext ) extends LazyLogging {
  val view: Agent[T] = Agent( zero )


}
