package omnibus.akka

import akka.actor.{ActorContext, ActorPath, ActorRef, ActorSelection}
import com.typesafe.scalalogging.StrictLogging
import omnibus.commons.identifier.ShortUUID


/**
 * The enveloping package creates an envelope that carries a great deal more information than just the sender of the message. 
 * The envelope contains the following:
 *   - The class type of the sender
 *   - The unique identifier of the sender
 *   - The unique identifier of the intended recipient
 *   - The class type of the message being sent
 *   - The timestamp of the creation time of the message
 *   - A unique identifier for the current “unit of work” in which this message is participating
 *   - A sequence number that indicates where this message sits in the timeline of the unit of work
 *   - A version number for the protocol version of the envelope
 *   - The message itself
 *
 * Importantly, all of this information to be supplied without burdening the code that sends the message. However, instead of
 * the regular Akka `tell` and `!` operations, programmers must use the `send` operation to wrap the message in a envelope.
 * {{{someActor send SomeMessage( "Hello there, Mr. Actor" )}}}
 */
package object envelope extends StrictLogging {
  /** The version of the envelope protocol */
  case class EnvelopeVersion( version: Int = 1 )
  
  /**
   * Defines the envelope schema version number. We shouldn't have to supply this, generally, just pull it in from the implicit
   * scope
   */
  implicit val envelopeVersion = EnvelopeVersion()


  /** Defines the "type" of component in a message (e.g. MessageForwarder) */
  case class ComponentType( componentType: String )

  object ComponentType {
    val unknown = ComponentType( "UnknownComponentType" )
  }
  

  /** Defines the identity of the given component (e.g. /path/to/MessageForwarder) */
  case class ComponentPath( path: String ) {
    def this( ap: ActorPath ) = this( ap.toString )
  }

  object ComponentPath {
    val unknown = ComponentPath( "UnknownComponentPath" )

    def apply( path: ActorPath ): ComponentPath = new ComponentPath( path )
  }
  

  /** Defines the type of message being sent (e.g. SendEmail) */
  case class MessageType( messageType: String )

  object MessageType {
    val unknown = MessageType( "UnknownMessageType" )
  }
  

  /** Defines the work identifier that this message is part of */
  case class WorkId( workId: ShortUUID = ShortUUID() ) {
    override def toString: String = workId.toString
  }

  object WorkId {
    val unknown = WorkId( ShortUUID.zero )
  }

  
  /** Defines the sequence number of this message within the workId */
  case class MessageNumber( value: Long ) {
    def increment: MessageNumber = MessageNumber( value + 1 )
    override def toString: String = value.toString
  }

  object MessageNumber {
    val unknown = MessageNumber( -1 )
  }

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
  implicit class EnvelopeSending( val underlying: ActorRef ) extends AnyVal {

    /**
     * Send a message enclosed in a message envelope containing meta date about the message.
     */
    def sendEnvelope( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit = {
      underlying.tell( update(envelope), sender )
    }

    /**
     * Send a message enclosed in a message envelope containing meta date about the message.
     */
    def !+( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit = sendEnvelope( envelope )( sender )

    /**
     * Forward a message enclosed in a message envelope containg meta data about the message.
     */
    def forwardEnvelope( envelope: Envelope )( implicit context: ActorContext ): Unit = underlying.forward( update(envelope) )

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


  implicit class EnvelopeSendingSelection( val underlying: ActorSelection ) extends AnyVal {

    /**
      * Send a message enclosed in a message envelope containing meta date about the message.
      */
    def sendEnvelope( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit = {
      underlying.tell( update(envelope), sender )
    }

    /**
      * Send a message enclosed in a message envelope containing meta date about the message.
      */
    def !+( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit = sendEnvelope( envelope )( sender )

    /**
      * Forward a message enclosed in a message envelope containg meta data about the message.
      */
    def forwardEnvelope( envelope: Envelope )( implicit context: ActorContext ): Unit = underlying.forward( update( envelope ) )

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
