package peds.akka.envelope

import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import akka.actor.{Props, ActorRef, Actor, ActorSystem, ActorLogging}
import org.scalatest.{FunSuiteLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import akka.pattern.pipe
import akka.util.Timeout
import akka.actor.Status.Failure
import peds.commons.log.Trace


object TestActor {
  case class Unhandled( message: Any )
}

class TestActor( target: ActorRef ) extends EnvelopingActor with ActorLogging {
  override def trace: Trace[_] = Trace[TestActor]

  override def receive: Receive = bare orElse around( wrapped )

  def bare: Receive = { case Envelope( "INITIAL", h ) => target ! Envelope( "REPLY", h ) }
  def wrapped: Receive = { case "ACTOR" => target send "REPLY" }

  override def unhandled( message: Any ): Unit = target ! TestActor.Unhandled( message )
}


class EnvelopeSendingSpec( _system: ActorSystem ) 
extends TestKit( _system ) 
with FunSuiteLike 
with Matchers 
with BeforeAndAfterAll 
with ImplicitSender
{
  def this() = this( ActorSystem( "EnvelopeSendingSpec" ) )

  val d = 500.millis
  implicit val t = Timeout( d )
  implicit val ec = system.dispatcher

  val target = TestProbe()
  val source = system.actorOf( Props( classOf[TestActor], target.ref ) )
  implicit val probe = TestProbe()
  // val real = TestActorRef[TestActor].underlyingActor

  override def afterAll: Unit = system.shutdown()

  // import peds.akka.envelope._
  import peds.commons.util._

  test( "EnvelopeSending should wrap a message with an envelope with unknown from non-actor" ) {
    source send "INITIAL"
    target.expectMsgPF( max = d, hint = "initial send" ) {
      case Envelope( "REPLY", h ) => {
        assert( h.fromComponentType == ComponentType.unknown )
        assert( h.fromComponentPath == ComponentPath.unknown )
        assert( h.toComponentPath == ComponentPath( source.path ) )
        assert( h.messageType == MessageType( classOf[String].safeSimpleName ) )
        assert( h.workId == WorkId.unknown )
        assert( h.messageNumber == MessageNumber( 0 ) )
        assert( h.version == EnvelopeVersion( 1 ) )
        assert( h.properties == Map.empty )
      }
    }
  }

  test( "EnvelopeSending should wrap a message with an envelope with source from actor" ) {
    source send "ACTOR"
    target.expectMsgPF( max = d, hint = "actor send" ) {
      case Envelope( "REPLY", h ) => {
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