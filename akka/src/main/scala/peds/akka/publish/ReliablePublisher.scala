package peds.akka.publish

import akka.actor.{ActorContext, ActorPath}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.persistence.AtLeastOnceDelivery.{UnconfirmedDelivery, UnconfirmedWarning}
import peds.akka.envelope._
import peds.commons.log.Trace


object ReliablePublisher {
  sealed trait Protocol
  case class ReliableMessage( deliveryId: Long, message: Envelope ) extends Protocol
  case class Confirm( deliveryId: Long ) extends Protocol

  case class RedeliveryFailedException( message: Any ) extends RuntimeException
}

trait ReliablePublisher extends EventPublisher { outer: PersistentActor with AtLeastOnceDelivery with Enveloping =>
  import ReliablePublisher._

  private val trace = Trace[ReliablePublisher]

  def reliablePublisher( destination: ActorPath )( implicit context: ActorContext ): Publisher = {
    ( event: Any ) => {
      val deliveryIdToMessage = (deliveryId: Long) => {
        log info s"ReliablePublisher.publish.DELIVER: deliveryId=${deliveryId}; dest=${destination}; event=${event}"
        ReliableMessage( deliveryId, event ) 
      }

      deliver( destination )( deliveryIdToMessage )
      Left( event )
    }
  }

  override def around( r: Receive ): Receive = {
    case Confirm( deliveryId ) => trace.block( "ReliablePublisher.around(Confirm)" ) {
      log info s"confirmed delivery: $deliveryId"
      confirmDelivery( deliveryId )
    }

    case UnconfirmedWarning( unconfirmed ) => trace.block( "ReliablePublisher.around(UnconfirmedWarning)" ) {
      log warning s"unconfirmed deliveries: ${unconfirmed}"
      handleUnconfirmedDeliveries( unconfirmed )
    }

    case msg => trace.block( "ReliablePublisher.around(_)" ) {
      super.around( r )( msg )
    }
  }

  def handleUnconfirmedDeliveries( unconfirmedDeliveries: Seq[UnconfirmedDelivery] ): Unit = trace.block( "handleUnconfirmedDeliveries" ) { 
    for { ud <- unconfirmedDeliveries } {
      log.warning( 
        s"unconfirmed delivery for message[${ud.message.getClass.getSimpleName}] " +
        s"to destination[${ud.destination}] with deliveryId=${ud.deliveryId}" 
      )
    }
  }
}
