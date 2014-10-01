package peds.akka.envelope

import scala.concurrent.Future
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern


trait AskSupport {
  implicit def ask( actorRef: ActorRef ): AskableEnvelopingActorRef = new AskableEnvelopingActorRef( actorRef )
}


final class AskableEnvelopingActorRef( val underlying: ActorRef ) extends AnyVal {
  
  def ask( message: Any )( implicit timeout: Timeout ): Future[Any] = akka.pattern.ask( underlying, update( message ) )

  def ?( message: Any )( implicit timeout: Timeout ): Future[Any] = ask( message )( timeout )

  private def update( incoming: Envelope ): Envelope = {
    val messageNumber = incoming.header.messageNumber
    val header = incoming.header.copy(
      toComponentPath = ComponentPath( underlying.path ), 
      messageNumber = messageNumber.increment 
    )

    incoming.copy( header = header )
  }
}
