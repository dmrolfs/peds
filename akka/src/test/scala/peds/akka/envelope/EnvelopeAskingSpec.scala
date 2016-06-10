package peds.akka.envelope

import scala.concurrent.Await
import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import akka.actor.{Props, ActorRef, ActorSystem, ActorLogging}
import org.scalatest.{FunSuiteLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import akka.util.Timeout
import peds.akka.envelope.pattern.ask


object EnvelopeAskingSpec {
  object TestActor {
    case class Unhandled( message: Any )
  }

  class TestActor( target: ActorRef ) extends EnvelopingActor with ActorLogging {
    override def receive: Receive = bare orElse around( wrapped )

    def bare: Receive = { 
      case "A" => sender() ! "REPLY FROM A" 
      case "B" => sender() sendEnvelope "REPLY FROM B"
      case Envelope( "C", h ) => sender() ! "REPLY FROM C" 
      case Envelope( "D", h ) => sender() sendEnvelope "REPLY FROM D"
    }

    def wrapped: Receive = { 
      case "E" => sender() sendEnvelope "REPLY FROM E"
      case "F" => sender() ! "REPLY FROM F" 
    }

    override def unhandled( message: Any ): Unit = target ! TestActor.Unhandled( message )
  }
}


class EnvelopeAskingSpec( _system: ActorSystem ) 
extends TestKit( _system ) 
with FunSuiteLike 
with Matchers 
with BeforeAndAfterAll 
with ImplicitSender
{
  def this() = this( ActorSystem( "EnvelopeAskingSpec" ) )

  import EnvelopeAskingSpec._

  val d = 500.millis
  implicit val t = Timeout( d )
  implicit val ec = system.dispatcher

  val target = TestProbe()
  val source = system.actorOf( Props( classOf[TestActor], target.ref ) )
  implicit val probe = TestProbe()
  // val real = TestActorRef[TestActor].underlyingActor

  override def afterAll: Unit = system.terminate()

  // import peds.akka.envelope._
  import peds.commons.util._

  test( "EnvelopeAsking should handle a regular message on ask" ) {
    val f = akka.pattern.ask( source, "A" )
    val r = Await.result( f, t.duration ).asInstanceOf[String]
    assert( r == "REPLY FROM A" )
  }

  test( "EnvelopeAsking should handle a regular ask message with wrapped if send with envelope" ) {
    val f = akka.pattern.ask( source, "B" )
    val Envelope( r, hd ) = Await.result( f, t.duration ).asInstanceOf[Envelope]
    assert( r == "REPLY FROM B" )
    assert( hd.fromComponentType == ComponentType( classOf[TestActor].safeSimpleName ) )
    assert( hd.fromComponentPath == ComponentPath( source.path) )
    assert( hd.toComponentPath.path.contains( "/temp/" ) )
    assert( hd.messageType == MessageType( classOf[String].safeSimpleName ) )
    assert( hd.workId == WorkId.unknown )
    assert( hd.messageNumber == MessageNumber( 0 ) )
    assert( hd.version == EnvelopeVersion( 1 ) )
    assert( hd.properties == Map.empty )
  }

  test( "EnvelopeAsking should wrap a message with an envelope on ask" ) {
    val fc = source ?+ "C"
    val rc = Await.result( fc, t.duration ).asInstanceOf[String]
    assert( rc == "REPLY FROM C" )
  }

  test( "EnvelopeAsking should reply with a wrapped message on ask if using send" ) {
    val fd = source ?+ "D"
    val Envelope( rd, hd ) = Await.result( fd, t.duration ).asInstanceOf[Envelope]
    assert( rd == "REPLY FROM D" )
    assert( hd.fromComponentType == ComponentType( classOf[TestActor].safeSimpleName ) )
    assert( hd.fromComponentPath == ComponentPath( source.path) )
    assert( hd.toComponentPath.path.contains( "/temp/" ) )
    assert( hd.messageType == MessageType( classOf[String].safeSimpleName ) )
    assert( hd.workId == WorkId.unknown )
    assert( hd.messageNumber == MessageNumber( 0 ) )
    assert( hd.version == EnvelopeVersion( 1 ) )
    assert( hd.properties == Map.empty )
  }

  test( "EnvelopeAsking should handle around processing wrapped message " ) {
    val future = source ?+ "E"
    val Envelope( result, h ) = Await.result( future, t.duration ).asInstanceOf[Envelope]
    assert( result == "REPLY FROM E" )
    assert( h.fromComponentType == ComponentType( classOf[TestActor].safeSimpleName ) )
    assert( h.fromComponentPath == ComponentPath( source.path ) )
    assert( h.toComponentPath.path.contains( "/temp/" ) )
    assert( h.messageType == MessageType( classOf[String].safeSimpleName ) )
    assert( h.workId != WorkId.unknown )
    assert( h.messageNumber == MessageNumber( 1 ) )
    assert( h.version == EnvelopeVersion( 1 ) )
    assert( h.properties == Map.empty )
  }

  test( "EnvelopeAsking should handle around processing wrapped message without envelope is not send" ) {
    val future = source ?+ "F"
    val result = Await.result( future, t.duration ).asInstanceOf[String]
    assert( result == "REPLY FROM F" )
  }
}
