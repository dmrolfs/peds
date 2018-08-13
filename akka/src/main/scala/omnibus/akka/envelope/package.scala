package omnibus.akka

import akka.actor.{ActorContext, ActorPath, ActorRef, ActorSelection}
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import omnibus.identifier.ShortUUID

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
package object envelope extends EnvelopeSyntax  {

  /** The version of the envelope protocol */
  @newtype class EnvelopeVersion( val version: Int )

  object EnvelopeVersion {
    def apply( version: Int = 1 ): EnvelopeVersion = version.coerce[EnvelopeVersion]
  }

  /** Defines the "type" of component in a message (e.g. MessageForwarder) */
  @newtype case class ComponentType( componentType: String )

  object ComponentType {
    val unknown = ComponentType( "UnknownComponentType" )
  }

  /** Defines the identity of the given component (e.g. /path/to/MessageForwarder) */
  @newtype case class ComponentPath( path: String )

  object ComponentPath {
    val unknown = ComponentPath( "UnknownComponentPath" )

    def apply( path: ActorPath ): ComponentPath = ComponentPath( path.toString )
  }

  /** Defines the type of message being sent (e.g. SendEmail) */
  @newtype case class MessageType( messageType: String )

  object MessageType {
    val unknown = MessageType( "UnknownMessageType" )
  }

  /** Defines the work identifier that this message is part of */
  @newtype class WorkId( val workId: ShortUUID ) {
    override def toString: String = workId.toString
  }

  object WorkId {
    def apply( id: ShortUUID = ShortUUID() ): WorkId = id.coerce[WorkId]
    val unknown = WorkId( ShortUUID.zero )
  }

  /** Defines the sequence number of this message within the workId */
  @newtype case class MessageNumber( value: Long ) {
    def increment: MessageNumber = MessageNumber( value + 1 )
    override def toString: String = value.toString
  }

  object MessageNumber {
    val unknown = MessageNumber( -1 )
  }

  @newtype class EnvelopeProperties( val properties: Map[Symbol, Any] )

  object EnvelopeProperties {
    def apply( properties: Map[Symbol, Any] = Map.empty[Symbol, Any] ): EnvelopeProperties = {
      properties.coerce[EnvelopeProperties]
    }
  }

  /**
    * Defines the envelope schema version number. We shouldn't have to supply this, generally, just pull it in from the implicit
    * scope
    */
  implicit val envelopeVersion = EnvelopeVersion()

  implicit val defaultEnvelopeProperties = EnvelopeProperties()
}
