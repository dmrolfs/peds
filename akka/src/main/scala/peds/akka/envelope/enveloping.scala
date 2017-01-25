package peds.akka.envelope

import akka.actor.{ Actor, ActorLogging }
import peds.akka.ActorStack
import peds.commons.util._


trait Enveloping { 
  def pathname: String

  @transient implicit val myComponentType: ComponentType = ComponentType( getClass.safeSimpleName )
  @transient implicit val myComponentPath: ComponentPath = ComponentPath( pathname )

  implicit def workId: WorkId = currentWorkIdVar
  protected def workId_=( wid: WorkId ): Unit = { currentWorkIdVar = if ( wid != WorkId.unknown ) wid else WorkId() }
  @transient private[this] var currentWorkIdVar: WorkId = WorkId.unknown

  implicit def messageNumber: MessageNumber = currentMessageNumberVar
  protected def messageNumber_=( messageNum: MessageNumber ): Unit = { currentMessageNumberVar = messageNum }
  @transient private[this] var currentMessageNumberVar: MessageNumber = MessageNumber.unknown

  def envelopeHeader: Option[EnvelopeHeader] = currentHeaderVar
  protected def envelopeHeader_=( header: Option[EnvelopeHeader] ): Unit = { currentHeaderVar = header }
  @transient private[this] var currentHeaderVar: Option[EnvelopeHeader] = None
}


trait EnvelopingActor extends ActorStack with Enveloping { outer: Actor with ActorLogging =>
  // override def pathname: String = self.path.name
  override def pathname: String = self.path.toString

  override def around( r: Receive ): Receive = {
    case Envelope( msg, header @ EnvelopeHeader( _, _, _, _, workId, messageNumber, _, _, _ ) ) => {
      this.envelopeHeader = Some( header )
      this.workId = workId
      this.messageNumber = messageNumber
      super.around( r )( msg )
    }

    case msg => {
      this.envelopeHeader = None
      this.workId = WorkId()
      this.messageNumber = MessageNumber( -1 )
      super.around( r )( msg )
    }
  }
}
