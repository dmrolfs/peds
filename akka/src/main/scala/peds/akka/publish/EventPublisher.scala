package peds.akka.publish

import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import peds.akka.envelope._


trait EventPublisher extends PersistentActor with ActorLogging {
  def publish( 
    event: Any
  )(
    implicit workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 )
  ): Unit

  def publishProtocol: Actor.Receive = Actor.emptyBehavior

  abstract override def receiveCommand: Actor.Receive = LoggingReceive {
    publishProtocol orElse {
      case c => super.receiveCommand( c )
    }
  }

  abstract override def receiveRecover: Actor.Receive = LoggingReceive {
    publishProtocol orElse {
      case event => super.receiveRecover( event )
    }
  }
}
