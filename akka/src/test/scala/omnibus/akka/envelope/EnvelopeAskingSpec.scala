package omnibus.akka.envelope

import scala.concurrent.duration._
import akka.testkit.TestProbe
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props }
import akka.util.Timeout
import omnibus.akka.envelope.EnvelopeAskingSpec.AskingTestActor
import org.scalatest.{ Matchers, Tag }
import omnibus.core.syntax.clazz._
import omnibus.akka.envelope.pattern.ask
import omnibus.akka.testkit.ParallelAkkaSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import journal._

object EnvelopeAskingSpec {

  object AskingTestActor {
    def props( target: ActorRef ): Props = Props( new AskingTestActor( target ) )
    case class Unhandled( message: Any )
  }

  class AskingTestActor( target: ActorRef ) extends Actor with EnvelopingActor with ActorLogging {
    override def receive: Receive = bare orElse around( wrapped )

    def bare: Receive = {
      case "A"                => sender() ! "REPLY FROM A"
      case "B"                => sender() sendEnvelope "REPLY FROM B"
      case Envelope( "C", _ ) => sender() ! "REPLY FROM C"
      case Envelope( "D", _ ) => sender() sendEnvelope "REPLY FROM D"
    }

    def wrapped: Receive = {
      case "E" => sender() sendEnvelope "REPLY FROM E"
      case "F" => sender() ! "REPLY FROM F"
    }

    override def unhandled( message: Any ): Unit = target ! TestActor.Unhandled( message )
  }
}

class EnvelopeAskingSpec extends ParallelAkkaSpec with Matchers with ScalaFutures {
  private val log = Logger[EnvelopeAskingSpec]

  class Fixture( override val slug: String, _system: ActorSystem )
      extends AkkaFixture( slug, _system ) {
    val d = 500.millis
    implicit val t = Timeout( d )
    val ec = system.dispatcher

    val target = TestProbe( "target" )
    val source = system.actorOf( Props( classOf[AskingTestActor], target.ref ), "source" )
    val probe = TestProbe( "probe" )
    // val real = TestActorRef[TestActor].underlyingActor

    override def after( test: OneArgTest ): Unit = {
      log.debug( s"killing fixture[${slug}]'s source actor:[${source.path}]..." )
      source ! PoisonPill
    }
  }

  override def createAkkaFixture( test: OneArgTest, system: ActorSystem, slug: String ): Fixture = {
    new Fixture( slug, system )
  }

  object WIP extends Tag( "wip" )

  "EnvelopingAskingActor" should {
    "handle a regular message on ask" in { f: Fixture =>
      import f._
      val answer = akka.pattern.ask( source, "A" )
      whenReady( answer, timeout( Span( 3, Seconds ) ) ) {
        case a: String =>
          a shouldBe "REPLY FROM A"
      }
    }

    "handle a regular ask message with wrapped if send with envelope" in { f: Fixture =>
      import f._

      val answer = akka.pattern.ask( source, "B" )
      whenReady( answer, timeout( Span( 3, Seconds ) ) ) {
        case Envelope( r, hd ) =>
          r shouldBe "REPLY FROM B"
          hd.fromComponentType shouldBe ComponentType( classOf[AskingTestActor].safeSimpleName )
          hd.fromComponentPath shouldBe ComponentPath( source.path )
          hd.toComponentPath.path should include( "/temp/" )
          hd.messageType shouldBe MessageType( classOf[String].safeSimpleName )
          hd.workId shouldBe WorkId.unknown
          hd.messageNumber shouldBe MessageNumber( 0 )
          hd.version shouldBe EnvelopeVersion( 1 )
          hd.properties shouldBe Map.empty[Symbol, Any]
      }
    }

    "wrap a message with an envelope on ask" in { f: Fixture =>
      import f._
      val answer = source ?+ "C"
      whenReady( answer, timeout( Span( 3, Seconds ) ) ) {
        case a: String =>
          a shouldBe "REPLY FROM C"
      }
    }

    "reply with a wrapped message on ask if using send" in { f: Fixture =>
      import f._
      val answer = source ?+ "D"
      whenReady( answer, timeout( Span( 3, Seconds ) ) ) {
        case Envelope( rd, hd ) =>
          rd shouldBe "REPLY FROM D"
          hd.fromComponentType shouldBe ComponentType( classOf[AskingTestActor].safeSimpleName )
          hd.fromComponentPath shouldBe ComponentPath( source.path )
          hd.toComponentPath.path should include( "/temp/" )
          hd.messageType shouldBe MessageType( classOf[String].safeSimpleName )
          hd.workId shouldBe WorkId.unknown
          hd.messageNumber shouldBe MessageNumber( 0 )
          hd.version shouldBe EnvelopeVersion( 1 )
          hd.properties shouldBe Map.empty[Symbol, Any]
      }
    }

    "handle around processing wrapped message " in { f: Fixture =>
      import f._

      val answer = source ?+ "E"
      whenReady( answer, timeout( Span( 3, Seconds ) ) ) {
        case Envelope( result, h ) =>
          result shouldBe "REPLY FROM E"
          h.fromComponentType shouldBe ComponentType( classOf[AskingTestActor].safeSimpleName )
          h.fromComponentPath shouldBe ComponentPath( source.path )
          h.toComponentPath.path should include( "/temp/" )
          h.messageType shouldBe MessageType( classOf[String].safeSimpleName )
          h.workId should not be (WorkId.unknown)
          h.messageNumber shouldBe MessageNumber( 1 )
          h.version shouldBe EnvelopeVersion( 1 )
          h.properties shouldBe Map.empty[Symbol, Any]
      }
    }

    "handle around processing wrapped message without envelope is not send" in { f: Fixture =>
      import f._

      val answer = source ?+ "F"
      whenReady( answer, timeout( Span( 3, Seconds ) ) ) {
        case a: String =>
          a shouldBe "REPLY FROM F"
      }
    }
  }
}
