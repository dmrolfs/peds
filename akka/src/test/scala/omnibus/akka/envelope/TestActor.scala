package omnibus.akka.envelope

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import omnibus.akka.envelope.TestActor.EnvelopeSnapshot

object TestActor {
  def props( target: ActorRef ): Props = Props( new TestActor( target ) )

  case class Unhandled( message: Any )

  case class EnvelopeSnapshot(
    pathname: String,
    requestId: RequestId,
    myComponentType: ComponentType,
    myComponentPath: ComponentPath,
    workId: WorkId,
    messageNumber: MessageNumber,
    envelopeProperties: EnvelopeProperties,
    envelopeHeader: Option[EnvelopeHeader],
  )
}

class TestActor( target: ActorRef ) extends Actor with EnvelopingActor with ActorLogging {
  override def receive: Receive = bare orElse around( wrapped )

  def snapshot: EnvelopeSnapshot = EnvelopeSnapshot(
    pathname = this.pathname,
    requestId = this.requestId,
    myComponentType = this.myComponentType,
    myComponentPath = this.myComponentPath,
    workId = this.workId,
    messageNumber = this.messageNumber,
    envelopeProperties = this.envelopeProperties,
    envelopeHeader = this.envelopeHeader
  )

  def bare: Receive = {
    case Envelope( "INITIAL", h ) => target ! Envelope( "REPLY", h )
  }

  def wrapped: Receive = {
    case "ACTOR"   => target sendEnvelope "REPLY"
    case "FORWARD" => target forwardEnvelope "REPLY"
    case "PLAIN"   => target ! "PLAIN REPLY"
  }

  override def unhandled( message: Any ): Unit = target ! TestActor.Unhandled( message )
}
