package omnibus.akka.envelope

import scala.language.implicitConversions
import akka.actor.{ ActorContext, ActorRef, ActorSelection }

trait EnvelopeSyntax {
  implicit def actorRefEnvelopeSending( ref: ActorRef ): EnvelopeSyntax.RefSending = {
    scribe.debug( s"actorRefEnvelopeSending: creating exposure for [${ref.path}]" )
    new EnvelopeSyntax.RefSending( ref )
  }

  implicit def actorSelectionEnvelopeSending( sel: ActorSelection ): EnvelopeSyntax.SelectionSending = {
    scribe.debug( s"actorSelectionEnvelopeSending: creating exposure for [${sel}]" )
    new EnvelopeSyntax.SelectionSending( sel )
  }
}

object EnvelopeSyntax {
  /**
    * This implementation is ported from <a href="http://derekwyatt.org/2014/01/01/using-scala-implicits-to-implement-a-messaging-protocol.html">Derek Wyatt's blog post</a>
    * Parts of this blog post are adapted here for documentation.
    *
    * EnvelopeSending is an implicit value type class that extends the functionality of the ActorRef to provide the hook that
    * wrap messages inside a rich envelope containg meta data about the message.
    *
    * With this implicit class in scope, `send(..)` a message to an ActorRef will result in wrapping the message in an Envelope.
    * `send` or `sendForward` must be used rather than the regular ActorRef's `tell`, `forward`, and `!` methods, which will
    * remain unwrapped with an envelope. It is easy to "drop" the envelope as a result, so as long as the envelope is important
    * care should be taken to use the enveloping methods.
    *
    *
    */
  final class RefSending( val underlying: ActorRef ) extends AnyVal {

    /**
      * Send a message enclosed in a message envelope containing meta date about the message.
      */
    def sendEnvelope(
      envelope: Envelope
    )(
      implicit sender: ActorRef = ActorRef.noSender
    ): Unit = {
      scribe.debug(
        s"sendEnvelope: envelope:[${envelope}] to underlying:[${underlying.path}] sender:[${sender.path}]"
      )
      underlying.tell( update( envelope ), sender )
    }

    /**
      * Send a message enclosed in a message envelope containing meta date about the message.
      */
    def !+( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit =
      sendEnvelope( envelope )( sender )

    /**
      * Forward a message enclosed in a message envelope containg meta data about the message.
      */
    def forwardEnvelope( envelope: Envelope )( implicit context: ActorContext ): Unit =
      underlying.forward( update( envelope ) )

    private def update( incoming: Envelope ): Envelope = {
      // val messageNumber = messageNumberLens.get( incoming )
      // emitLens.set( incoming )( (ComponentPath( underlying.path ), messageNumber.increment) )
      // val workId = if ( incoming.header.workId == WorkId.unknown ) WorkId() else incoming.header.workId

      val messageNumber = incoming.header.messageNumber

      val header = incoming.header.copy(
        toComponentPath = ComponentPath( underlying.path ),
        // workId = workId,
        messageNumber = messageNumber.increment
      )

      incoming.copy( header = header )
    }
  }

  final class SelectionSending( val underlying: ActorSelection ) extends AnyVal {

    /**
      * Send a message enclosed in a message envelope containing meta date about the message.
      */
    def sendEnvelope(
      envelope: Envelope
    )(
      implicit sender: ActorRef = ActorRef.noSender
    ): Unit = {
      underlying.tell( update( envelope ), sender )
    }

    /**
      * Send a message enclosed in a message envelope containing meta date about the message.
      */
    def !+( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit =
      sendEnvelope( envelope )( sender )

    /**
      * Forward a message enclosed in a message envelope containg meta data about the message.
      */
    def forwardEnvelope( envelope: Envelope )( implicit context: ActorContext ): Unit =
      underlying.forward( update( envelope ) )

    private def update( incoming: Envelope ): Envelope = {
      // val messageNumber = messageNumberLens.get( incoming )
      // emitLens.set( incoming )( (ComponentPath( underlying.path ), messageNumber.increment) )
      // val workId = if ( incoming.header.workId == WorkId.unknown ) WorkId() else incoming.header.workId

      val messageNumber = incoming.header.messageNumber

      val header = incoming.header.copy(
        toComponentPath = ComponentPath( underlying.anchorPath ),
        // workId = workId,
        messageNumber = messageNumber.increment
      )

      incoming.copy( header = header )
    }
  }
}
