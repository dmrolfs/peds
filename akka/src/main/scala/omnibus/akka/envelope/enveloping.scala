package omnibus.akka.envelope

import akka.actor.Actor
import omnibus.akka.ActorStack
import omnibus.core.syntax.clazz._

trait Enveloping {
  def pathname: String

  @transient implicit val myComponentType: ComponentType = ComponentType( getClass.safeSimpleName )
  @transient implicit val myComponentPath: ComponentPath = ComponentPath( pathname )

  implicit def workId: WorkId = currentWorkIdVar
  protected def workId_=( wid: WorkId ): Unit = {
    currentWorkIdVar = if (wid != WorkId.unknown) wid else WorkId()
  }
  @transient private[this] var currentWorkIdVar: WorkId = WorkId.unknown

  implicit def messageNumber: MessageNumber = currentMessageNumberVar
  protected def messageNumber_=( messageNum: MessageNumber ): Unit = {
    currentMessageNumberVar = messageNum
  }
  @transient private[this] var currentMessageNumberVar: MessageNumber = MessageNumber.unknown

  implicit def envelopeProperties: EnvelopeProperties = currentEnvelopeProperties
  protected def envelopeProperties_=( properties: EnvelopeProperties ): Unit = {
    currentEnvelopeProperties = properties
  }

  @transient private[this] var currentEnvelopeProperties: EnvelopeProperties = {
    emptyEnvelopeProperties
  }

  def envelopeHeader: Option[EnvelopeHeader] = currentHeaderVar
  protected def envelopeHeader_=( header: Option[EnvelopeHeader] ): Unit = {
    currentHeaderVar = header
  }
  @transient private[this] var currentHeaderVar: Option[EnvelopeHeader] = None

  implicit def requestId: RequestId = RequestId.summon()
}

trait EnvelopingActor extends ActorStack with Enveloping { outer: Actor =>
  // override def pathname: String = self.path.name
  override def pathname: String = self.path.toString

  override def around( r: Receive ): Receive = {
    case Envelope(
        msg,
        header @ EnvelopeHeader( _, _, _, _, workId, messageNumber, _, properties, _ )
        ) => {
      this.envelopeHeader = Some( header )
      this.workId = workId
      this.messageNumber = messageNumber
      this.envelopeProperties = properties
      super.around( r )( msg )
    }

    case msg => {
      this.envelopeHeader = None
      this.workId = WorkId()
      this.messageNumber = MessageNumber( -1 )
      this.envelopeProperties = emptyEnvelopeProperties
      super.around( r )( msg )
    }
  }
}
