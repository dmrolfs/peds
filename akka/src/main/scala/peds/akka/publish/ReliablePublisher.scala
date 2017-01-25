package peds.akka.publish

import akka.actor.{ActorContext, ActorPath, ActorLogging}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.persistence.AtLeastOnceDelivery.{UnconfirmedDelivery, UnconfirmedWarning}
import peds.akka.envelope._
import peds.commons.util._


object ReliablePublisher {
  sealed trait Protocol
  case class ReliableMessage( deliveryId: Long, message: Envelope ) extends Protocol
  case class Confirm( deliveryId: Long ) extends Protocol

  case class RedeliveryFailedException( message: Any ) extends RuntimeException
}

trait ReliablePublisher extends EventPublisher { outer: PersistentActor with AtLeastOnceDelivery with Enveloping with ActorLogging =>
  import ReliablePublisher._

  def reliablePublisher( destination: ActorPath )( implicit context: ActorContext ): Publisher = {
    ( event: Any ) => {
      val deliveryIdToMessage = (deliveryId: Long) => {
        log.info( "ReliablePublisher.publish.DELIVER: deliveryId=[{}] dest=[{}] event=[{}]", deliveryId, destination, event )
        ReliableMessage( deliveryId, event ) 
      }

      deliver( destination )( deliveryIdToMessage )
      Left( event )
    }
  }

  override def around( r: Receive ): Receive = {
    case Confirm( deliveryId ) => {
      log.info( "confirmed delivery: [{}}]", deliveryId )
      confirmDelivery( deliveryId )
    }

    case UnconfirmedWarning( unconfirmed ) => {
      log.warning( "unconfirmed deliveries: [{}]", unconfirmed )
      handleUnconfirmedDeliveries( unconfirmed )
    }

    case msg => super.around( r )( msg )
  }

  def handleUnconfirmedDeliveries( unconfirmedDeliveries: Seq[UnconfirmedDelivery] ): Unit = { 
    for { ud <- unconfirmedDeliveries } {
      log.warning( 
        "unconfirmed delivery for message[{}] to destination[{}] with deliveryId={}",
        ud.message.getClass.safeSimpleName, ud.destination, ud.deliveryId
      )
    }
  }
}
