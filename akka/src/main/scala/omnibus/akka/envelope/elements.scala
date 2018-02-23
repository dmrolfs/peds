package omnibus.akka.envelope

import akka.actor.ActorPath
import omnibus.commons.identifier.ShortUUID


/** The version of the envelope protocol */
case class EnvelopeVersion( version: Int = 1 )

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


case class RequestId( trackingId: String, spanId: String )

object RequestId {
  def summon()( implicit workId: WorkId, messageNumber: MessageNumber ): RequestId = {
    RequestId( trackingId = workId.toString, spanId = messageNumber.toString )
  }
}
