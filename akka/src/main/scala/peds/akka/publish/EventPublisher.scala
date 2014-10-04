package peds.akka.publish

import akka.actor.{ Actor, ActorLogging }
import peds.akka.ActorStack
import peds.akka.envelope._


trait EventPublisher extends ActorStack with ActorLogging {
  //todo: refactor this into a Functional form that can be composed
  def publish( 
    event: Any
  )(
    implicit workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 )
  ): Unit
}
