package omnibus.akka.envelope

import scala.concurrent.duration._
import akka.util.Timeout
import akka.testkit.TestProbe
import akka.actor.{ ActorSystem, PoisonPill, Props }
import omnibus.akka.testkit.ParallelAkkaSpec
import org.scalatest.{ Matchers, Tag }
import journal._
import omnibus.core.syntax.clazz._

class EnvelopeSendingSpec extends ParallelAkkaSpec with Matchers {
  private val log = Logger[EnvelopeSendingSpec]

  class Fixture( override val slug: String, _system: ActorSystem )
      extends AkkaFixture( slug, _system ) {
    val d = 500.millis
    implicit val t = Timeout( d )
    val target = TestProbe()
    val source = system.actorOf( Props( classOf[TestActor], target.ref ) )
    implicit val probe = TestProbe()
    override def after( test: OneArgTest ): Unit = {
      log.debug( s"killing fixture[${slug}]'s source actor:[${source.path}]..." )
      source ! PoisonPill
    }
  }

  override def createAkkaFixture( test: OneArgTest, system: ActorSystem, slug: String ): Fixture = {
    new Fixture( slug, system )
  }

  object WIP extends Tag( "wip" )

  "EnvelopeSending" should {
    "wrap a message with an envelope with unknown from non-actor" in { f: Fixture =>
      import f._
      source.sendEnvelope( "INITIAL" )( probe.ref )
      target.expectMsgPF( max = d, hint = "initial send" ) {
        case Envelope( "REPLY", h ) => {
          target.sender() shouldBe source
          h.fromComponentType shouldBe ComponentType.noSender
          h.fromComponentPath shouldBe ComponentPath.noSender
          h.toComponentPath shouldBe ComponentPath( source.path )
          h.messageType shouldBe MessageType( classOf[String].safeSimpleName )
          h.workId should not be (WorkId.unknown)
          h.messageNumber shouldBe MessageNumber( 0 )
          h.version shouldBe EnvelopeVersion( 1 )
          h.properties.properties shouldBe empty
        }
      }
    }

    "wrap a message with an envelope with source from actor" in { f: Fixture =>
      import f._
      source.sendEnvelope( "ACTOR" )( probe.ref )
      target.expectMsgPF( max = d, hint = "actor send" ) {
        case Envelope( "REPLY", h ) => {
          target.sender() shouldBe source
          h.fromComponentType shouldBe ComponentType( classOf[TestActor].safeSimpleName )
          h.fromComponentPath shouldBe ComponentPath( source.path )
          h.toComponentPath shouldBe ComponentPath( target.ref.path )
          h.messageType shouldBe MessageType( classOf[String].safeSimpleName )
          h.workId should not be (WorkId.unknown)
          h.messageNumber shouldBe MessageNumber( 1 )
          h.version shouldBe EnvelopeVersion( 1 )
          h.properties.properties shouldBe empty
        }
      }
    }

    "wrap a forwarded message with an envelope with source from actor" in { f: Fixture =>
      import f._
      source.sendEnvelope( "FORWARD" )( probe.ref )
      target.expectMsgPF( max = d, hint = "actor send" ) {
        case Envelope( "REPLY", h ) => {
          target.sender() shouldBe probe.ref
          h.fromComponentType shouldBe ComponentType( classOf[TestActor].safeSimpleName )
          h.fromComponentPath shouldBe ComponentPath( source.path )
          h.toComponentPath shouldBe ComponentPath( target.ref.path )
          h.messageType shouldBe MessageType( classOf[String].safeSimpleName )
          h.workId should not be (WorkId.unknown)
          h.messageNumber shouldBe MessageNumber( 1 )
          h.version shouldBe EnvelopeVersion( 1 )
          h.properties.properties shouldBe empty
        }
      }
    }

    // test( "An ActorStack request should pass messages not understood to unhandled" ) {
    //   probe.send( source, "MYSTERY" )
    //   target expectMsg TestActor.Unhandled( "MYSTERY" )
    // }
  }

}
