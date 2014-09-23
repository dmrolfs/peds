package peds.akka.publish

import akka.actor.{ Actor, ActorLogging }
import peds.akka.ActorStack
import peds.akka.envelope._


trait EventPublisher extends ActorStack with ActorLogging {
  def publish( 
    event: Any
  )(
    implicit workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 )
  ): Unit
}
