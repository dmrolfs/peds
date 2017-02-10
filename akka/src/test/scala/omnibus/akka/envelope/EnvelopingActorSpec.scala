package omnibus.akka.envelope

import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import akka.actor.{Actor, Props, ActorRef, ActorSystem, ActorLogging}
import org.scalatest.{FunSuiteLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import akka.util.Timeout


object EnvelopingActorSpec {
  object TestActor {
    case class Unhandled( message: Any )
  }

  class TestActor( target: ActorRef ) extends Actor with EnvelopingActor with ActorLogging {
    override def receive: Receive = bare orElse around( wrapped )

    def bare: Receive = { 
      case Envelope( "INITIAL", h ) => target ! Envelope( "REPLY", h ) 
    }

    def wrapped: Receive = { 
      case "ACTOR" => target sendEnvelope "REPLY"
      case "FORWARD" => target forwardEnvelope "REPLY"
      case "PLAIN" => target ! "PLAIN REPLY"
    }

    override def unhandled( message: Any ): Unit = target ! TestActor.Unhandled( message )
  }
}


class EnvelopingActorSpec( _system: ActorSystem ) 
extends TestKit( _system ) 
with FunSuiteLike 
with Matchers 
with BeforeAndAfterAll 
with ImplicitSender
{
  def this() = this( ActorSystem( "EnvelopeSendingSpec" ) )

  import EnvelopingActorSpec._

  val d = 500.millis
  implicit val t = Timeout( d )
  implicit val ec = system.dispatcher

  val target = TestProbe()
  val source = system.actorOf( Props( classOf[TestActor], target.ref ) )
  implicit val probe = TestProbe()

  override def afterAll: Unit = system.terminate()

  // import omnibus.akka.envelope._
  import omnibus.commons.util._

  test( "should receive a wrapped message with unknown from non-actor" ) {
    source.sendEnvelope( "INITIAL" )( probe.ref )
    target.expectMsgPF( max = d, hint = "initial send" ) {
      case Envelope( "REPLY", h ) => {
        assert( target.sender() == source )
        assert( h.fromComponentType == ComponentType.unknown )
        assert( h.fromComponentPath == ComponentPath.unknown )
        assert( h.toComponentPath == ComponentPath( source.path ) )
        assert( h.messageType == MessageType( classOf[String].safeSimpleName ) )
        assert( h.workId != WorkId.unknown )
        assert( h.messageNumber == MessageNumber( 0 ) )
        assert( h.version == EnvelopeVersion( 1 ) )
        assert( h.properties == Map.empty )
      }
    }
  }

  test( "should receive a plain message with unknown from non-actor" ) {
    source.tell( "PLAIN", probe.ref )
    target expectMsg "PLAIN REPLY"
  }

  test( "should wrap a message with an envelope with source from actor" ) {
    source.sendEnvelope( "ACTOR" )( probe.ref )
    target.expectMsgPF( max = d, hint = "actor send" ) {
      case Envelope( "REPLY", h ) => {
        assert( target.sender() == source )
        assert( h.fromComponentType == ComponentType( classOf[TestActor].safeSimpleName ) )
        assert( h.fromComponentPath == ComponentPath( source.path ) )
        assert( h.toComponentPath == ComponentPath( target.ref.path ) )
        assert( h.messageType == MessageType( classOf[String].safeSimpleName ) )
        assert( h.workId != WorkId.unknown )
        assert( h.messageNumber == MessageNumber( 1 ) )
        assert( h.version == EnvelopeVersion( 1 ) )
        assert( h.properties == Map.empty )
      }
    }
  }

  test( "should wrap a forwarded message with an envelope with source from actor" ) {
    source.sendEnvelope( "FORWARD" )( probe.ref )
    target.expectMsgPF( max = d, hint = "actor send" ) {
      case Envelope( "REPLY", h ) => {
        assert( target.sender() == probe.ref )
        assert( h.fromComponentType == ComponentType( classOf[TestActor].safeSimpleName ) )
        assert( h.fromComponentPath == ComponentPath( source.path ) )
        assert( h.toComponentPath == ComponentPath( target.ref.path ) )
        assert( h.messageType == MessageType( classOf[String].safeSimpleName ) )
        assert( h.workId != WorkId.unknown )
        assert( h.messageNumber == MessageNumber( 1 ) )
        assert( h.version == EnvelopeVersion( 1 ) )
        assert( h.properties == Map.empty )
      }
    }
  }

  // test( "An ActorStack request should pass messages not understood to unhandled" ) {
  //   probe.send( source, "MYSTERY" )
  //   target expectMsg TestActor.Unhandled( "MYSTERY" )
  // }
}
