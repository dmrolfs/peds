package omnibus.akka.publish

import akka.actor.{ Actor, ActorLogging }
import omnibus.akka.ActorStack


trait ReliableReceiver extends ActorStack { outer: Actor with ActorLogging =>
  override def around( r: Receive ): Receive = {
    case ReliablePublisher.ReliableMessage( deliveryId, message ) => {
      log.info( "ReliableReceiver.ReliableMessage(deliveryId=[{}], message=[{}])", deliveryId, message )
      super.around( r )( message )
      sender() ! ReliablePublisher.Confirm( deliveryId )  //DMR: send confirmation after hanlding receive?  or before?
    }

    case msg => super.around( r )( msg )
  }
}
