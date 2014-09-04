package peds.akka.publish

import akka.actor.{ ActorLogging, ActorPath }
import akka.persistence.{ AtLeastOnceDelivery, PersistentActor }
import akka.persistence.AtLeastOnceDelivery.{ UnconfirmedDelivery, UnconfirmedWarning }
import peds.akka.envelope._


trait ReliablePublisher extends PersistentActor with AtLeastOnceDelivery with EventPublisher with ActorLogging { 
  outer: Enveloping =>

  def destination: ActorPath

  //DMR don't want to override default b/h here. instead, concrete class can
  // override val redeliverInterval: FiniteDuration = 30.seconds
  // override val warnAfterNumberOfUnconfirmedAttempts: Int = 15

  // override def preStart(): Unit = {
  //   super.preStart()
  //   val listener = context.actorOf( RedeliverFailureListener.props )
  // }

  override def publish( 
    event: Any 
  )(
    implicit workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 )
  ): Unit = {
    // val envelope = AggregateRoot.message2AggregateEnvelope( event, outer.meta.aggregateIdOf( self ), outer.meta )

    deliver( 
      destination, 
      deliveryId => {
        log info s"ReliablePublisher.publish.DELIVER: deliveryId=${deliveryId}; dest=${destination}; event=${event}"
        ReliableMessage( deliveryId, event ) 
      }
    )
  }


  override def publishProtocol: Receive = {
    case Confirm( deliveryId ) => {
      log info s"confirmed delivery: $deliveryId"
      confirmDelivery( deliveryId )
    }

    case UnconfirmedWarning( unconfirmedDeliveries ) => {
      log warning s"unconfirmed deliveries: ${unconfirmedDeliveries}"
      handleUnconfirmedDeliveries( unconfirmedDeliveries )
    }
  }

  def handleUnconfirmedDeliveries( unconfirmedDeliveries: Seq[UnconfirmedDelivery] ): Unit = { 
    for { ud <- unconfirmedDeliveries } {
      log.warning( 
        s"unconfirmed delivery for message[${ud.message.getClass.getSimpleName}] " +
        s"to destination[${ud.destination}] with deliveryId=${ud.deliveryId}" 
      )
      // confirmDelivery( ud.deliveryId )
    }
  }
}
