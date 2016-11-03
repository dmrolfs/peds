package peds.akka.publish

import akka.actor.ActorLogging
import peds.akka.ActorStack


trait ReliableReceiver extends ActorStack { outer: ActorLogging =>
  override def around( r: Receive ): Receive = {
    case ReliablePublisher.ReliableMessage( deliveryId, message ) => {
      log info s"ReliableReceiver.ReliableMessage(deliveryId=${deliveryId}, message=${message})"
      super.around( r )( message )
      sender() ! ReliablePublisher.Confirm( deliveryId )  //DMR: send confirmation after hanlding receive?  or before?
    }

    case msg => super.around( r )( msg )
  }
}
