package omnibus.akka.envelope.pattern

import scala.concurrent.Future
import akka.actor.ActorRef
import akka.util.Timeout
import omnibus.akka.envelope.{ComponentPath, Envelope}


trait AskSupport {
  import scala.language.implicitConversions
  implicit def ask( actorRef: ActorRef ): AskableEnvelopingActorRef = new AskableEnvelopingActorRef( actorRef )
}


final class AskableEnvelopingActorRef( val underlying: ActorRef ) extends AnyVal {
  
  def askEnvelope( message: Envelope )( implicit timeout: Timeout ): Future[Any] = {
    akka.pattern.ask( underlying, askUpdate(message) )
  }

  def ?+( message: Envelope )( implicit timeout: Timeout ): Future[Any] = askEnvelope( message )( timeout )

  private def askUpdate( incoming: Envelope ): Envelope = {
    val messageNumber = incoming.header.messageNumber
    val header = incoming.header.copy(
      toComponentPath = ComponentPath.unknown, // we don't have a way to determine the sender from underlying
      messageNumber = messageNumber.increment 
    )

    incoming.copy( header = header )
  }
}
