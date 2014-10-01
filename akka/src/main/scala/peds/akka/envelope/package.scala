package peds.akka

import akka.actor.{ ActorContext, ActorPath, ActorRef }
import com.typesafe.scalalogging.StrictLogging
import peds.commons.identifier.ShortUUID
import peds.commons.log.Trace


package object envelope extends AskSupport with StrictLogging {
  val trace = Trace( "Envelope", logger )

  // The version of the envelope protocol
  case class EnvelopeVersion( version: Int = 1 )
  
  // We shouldn't have to supply this, generally, just pull it in from
  // the implicit scope
  implicit val envelopeVersion = EnvelopeVersion()


  // Defines the "type" of component in a message (e.g. MessageForwarder)
  case class ComponentType( componentType: String )

  object ComponentType {
    val unknown = ComponentType( "UnknownComponentType" )
  }
  

  // Defines the identity of the given component (e.g. /path/to/MessageForwarder)
  case class ComponentPath( path: String ) {
    def this( ap: ActorPath ) = this( ap.elements.mkString( "/", "/", "" ) ) // DMR DRY THIS UP

  // // Rips off the unneeded bits from the Actor path in order to
  // // create a unique Id
  // def makeAnIdForActorRef(path: ActorPath): String = path.elements.mkString("/", "/", "")
  }

  object ComponentPath {
    val unknown = ComponentPath( "UnknownComponentPath" )

    def apply( path: ActorPath ): ComponentPath = new ComponentPath( path )
  }
  

  // Defines the type of message being sent (e.g. SendEmail)
  case class MessageType( messageType: String )

  object MessageType {
    val unknown = MessageType( "UnknownMessageType" )
  }
  

  // Defines the work identifier that this message is part of
  case class WorkId( workId: ShortUUID = ShortUUID() )

  object WorkId {
    val unknown = WorkId( ShortUUID.nilUUID )
  }

  
  // Defines the sequence number of this message within the workId
  case class MessageNumber( value: Long ) {
    def increment: MessageNumber = MessageNumber( value + 1 )
  }

  object MessageNumber {
    val unknown = MessageNumber( -1 )
  }

  // Here we create an implicit class that extends the functionality of the
  // ActorRef, which will provide the hook we need in order to convert from the
  // Any to the Envelope that holds all of our interesting information
  implicit class EnvelopeSending( val underlying: ActorRef ) extends AnyVal {
    def send( envelope: Envelope )( implicit sender: ActorRef = ActorRef.noSender ): Unit = trace.block( s"send( $envelope )( $sender )" ){
      trace( s"update(envelope) = ${update(envelope)}" )
      underlying.tell( update( envelope ), sender )
    }

    def sendForward( envelope: Envelope )( implicit context: ActorContext ): Unit = underlying.forward( update( envelope ) )


    import shapeless.{ Lens => SLens, _ }
    import peds.commons._

    // private def toComponentPathLens: SLens[Envelope, ComponentPath] = Lenser[Envelope].header.toComponentPath
    // private def toComponentPathLens: SLens[Envelope, ComponentPath] = new SLens[Envelope, ComponentPath] {
    //   override def get( that: Envelope ): ComponentPath = that.header.toComponentPath
    //   override def set( that: Envelope )( path: ComponentPath ): Envelope = {
    //     val header = that.header.toComponentPathLens.set( that.header )( path )
    //     // val header: EnvelopeHeader = that.header.copy( toComponentPath = path )
    //     val result: Envelope = that.copy( header = header )
    //     result
    //   }
    // }

    // private def messageNumberLens: SLens[Envelope, MessageNumber] = Lenser[Envelope].header.messageNumber
    // private def messageNumberLens: SLens[Envelope, MessageNumber] = new SLens[Envelope, MessageNumber] {
    //   override def get( that: Envelope ): MessageNumber = that.header.messageNumber
    //   override def set( that: Envelope )( number: MessageNumber ): Envelope = {
    //     val header = that.header.messageNumberLens.set( that.header )( number )
    //     headerLens.set( that )( header )
    //     // that.copy( header = header )
    //   }
    // }

    // private def emitLens = toComponentPathLens ~ messageNumberLens

    private def update( incoming: Envelope ): Envelope = trace.block( s"update($incoming)" ) {
      // val messageNumber = messageNumberLens.get( incoming )
      // emitLens.set( incoming )( (ComponentPath( underlying.path ), messageNumber.increment) )
      // val workId = if ( incoming.header.workId == WorkId.unknown ) WorkId() else incoming.header.workId

      val messageNumber = incoming.header.messageNumber
      // val messageNumber = (
      //   if ( incoming.header.messageNumber.value == -1 ) incoming.header.messageNumber.increment 
      //   else incoming.header.messageNumber
      // )

      val header = incoming.header.copy( 
        toComponentPath = ComponentPath( underlying.path ), 
        // workId = workId,
        messageNumber = messageNumber.increment 
      )

      incoming.copy( header = header )
    }
  }
}
