package peds.akka.publish

import akka.actor.{ Actor, ActorLogging }
import shapeless.syntax.typeable._
import peds.akka.envelope._


trait LocalPublisher extends EventPublisher {
  override def publish(
    event: Any
  )(
    implicit workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 )
  ): Unit = {
    log.info( s"local publish by ${self.path}: $event" )
    val target = context.system.eventStream
    event.cast[target.Event] foreach { e =>
      log.info( s"local stream publishing event:${e} on target:${target}" )
      //DMR: somehow need to update envelope per EnvelopeSending.update
      target publish e
    }
  }
}
