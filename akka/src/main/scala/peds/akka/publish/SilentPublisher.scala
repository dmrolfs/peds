package peds.akka.publish

import akka.actor.{ Actor, ActorLogging }
import peds.akka.envelope._


trait SilentPublisher extends EventPublisher { outer: Actor with ActorLogging =>
  override def publish(
    event: Any
  )(
    implicit workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 )
  ): Unit = { log.info( s"silent publish by ${self.path}: $event" ) }
}
