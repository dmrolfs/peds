package peds.akka.envelope

import akka.actor.{ Actor, ActorLogging }
import akka.event.LoggingReceive
import peds.akka.ActorStack
import peds.commons.log.Trace
import peds.commons.util._


// trait EnvelopingActor extends Actor with EnvelopeImplicits with ActorRefImplicits {
trait Enveloping { 
  def pathname: String

  @transient implicit val myComponentType: ComponentType = ComponentType( getClass.safeSimpleName )
  @transient implicit val myComponentPath: ComponentPath = ComponentPath( pathname )

  implicit def workId: WorkId = currentWorkIdVar
  protected def workId_=( wid: WorkId ): Unit = { currentWorkIdVar = wid }
  @transient private[this] var currentWorkIdVar: WorkId = WorkId()

  implicit def messageNumber: MessageNumber = currentMessageNumberVar
  protected def messageNumber_=( messageNum: MessageNumber ): Unit = { currentMessageNumberVar = messageNum }
  @transient private[this] var currentMessageNumberVar: MessageNumber = MessageNumber( -1 )

  def envelopeHeader: Option[EnvelopeHeader] = currentHeaderVar
  protected def envelopeHeader_=( header: Option[EnvelopeHeader] ): Unit = { currentHeaderVar = header }
  @transient private[this] var currentHeaderVar: Option[EnvelopeHeader] = None
}


trait EnvelopingActor extends Actor with ActorStack with Enveloping { outer: ActorLogging =>
  def trace: Trace[_]
  // override def path: String = self.path.elements.mkString( "/", "/", "" ) // DMR DRY THIS UP
  override def pathname: String = self.path.name

  override def around( r: Receive ): Receive = {
    case Envelope( msg, header @ EnvelopeHeader( _, _, _, _, workId, messageNumber, _, _, _ ) ) => {
log info s"EnvelopingActor.around caught Envelop( msg=${msg}, workId=${workId} )"
      this.envelopeHeader = Some( header )
      this.workId = workId
      this.messageNumber = messageNumber
      super.around( r )( msg )
    }

    case msg => {
log info s"EnvelopingActor.around caught msg( msg=${msg}, NEW workId=${workId} )"
      this.envelopeHeader = None
      this.workId = WorkId()
      this.messageNumber = MessageNumber( -1 )
      super.around( r )( msg )
    }
  }

  // override final def receive = aroundReceive( accept ) //DMR look to akka persistence to det how to allow receive() override rt accept()
}


// trait EnvelopingPersistentActor extends PersistentActor with ActorStack with Enveloping { outer: ActorLogging =>
//   override def pathname: String = self.path.name

//   override def wrappedReceiveRecover: Receive = Actor.emptyBehavior

//   abstract override def around( r: Receive ): Receive = LoggingReceive {
//     case Envelope( msg, header @ EnvelopeHeader( _, _, _, _, workId, messageNumber, _, _, _ ) ) => {
// log error s"EnvelopingPersistentActor.receiveCommand ENVELOPE RECD: hdr:${header} msg:${msg}"
//       this.envelopeHeader = Some( header )
//       this.workId = workId
//       this.messageNumber = messageNumber
//       super.aroundReceiveCommand( r )( msg )
//     }

//     case msg => {
// log error s"EnvelopingPersistentActor.receiveCommand UNKNOWN RECD: msg:${msg}"
//       this.envelopeHeader = None
//       this.workId = WorkId()
//       this.messageNumber = MessageNumber( -1 )
//       super.aroundReceiveCommand( r )( msg )
//     }
//   }
// }
