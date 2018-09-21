package omnibus.akka.envelope

import scala.concurrent.duration._
import akka.util.Timeout
import akka.testkit.{ TestActorRef, TestProbe }
import akka.actor.{ ActorSystem, PoisonPill, Props }
import omnibus.akka.testkit.ParallelAkkaSpec
import org.scalatest.{ Matchers, OptionValues, Tag }
import journal._
import omnibus.core.syntax.clazz._
import omnibus.identifier.ShortUUID

class EnvelopingActorSpec extends ParallelAkkaSpec with Matchers with OptionValues {
  private val log = Logger[EnvelopingActorSpec]

  class Fixture( override val slug: String, _system: ActorSystem )
      extends AkkaFixture( slug, _system ) {

    val d = 500.millis
    val t = Timeout( d )
    val probe = TestProbe( "probe" )
    val target = TestProbe( "target" )
    val source = system.actorOf( Props( classOf[TestActor], target.ref ), "source" )

    override def after( test: OneArgTest ): Unit = {
      log.debug( s"killing fixture[${slug}]'s source actor:[${source.path}]..." )
      source ! PoisonPill
    }
  }

  override def createAkkaFixture( test: OneArgTest, system: ActorSystem, slug: String ): Fixture = {
    new Fixture( slug, system )
  }

  object WIP extends Tag( "wip" )

  "EnvelopingActor" should {
    "verify default envelope elements" in { f: Fixture =>
      ComponentType.noSender.componentType shouldBe "no-sender"
      ComponentPath.noSender shouldBe "no-sender"
      val wid = WorkId()
      val zero = ShortUUID.zero
      val widUUID = wid.workId
      widUUID should not be (zero)
      WorkId().workId should not be (ShortUUID.zero)
      MessageNumber( -1 ).value shouldBe -1L
      EnvelopeVersion().version shouldBe 1
    }

    "receive a wrapped message with unknown from non-actor" in { f: Fixture =>
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

    "receive a plain message with unknown from non-actor" in { f: Fixture =>
      import f._
      source.tell( "PLAIN", probe.ref )
      target expectMsg "PLAIN REPLY"
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

      implicit val props = EnvelopeProperties( 'foo -> "bar", 'zed -> 17L )

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
          h.properties.properties shouldBe props
        }
      }
    }

    "update internal access to envelope header" taggedAs (WIP) in { f: Fixture =>
      import f._
      implicit val props = EnvelopeProperties( 'foo -> "bar", 'zed -> 17L )

      val ref = TestActorRef[TestActor]( TestActor.props( target.ref ), "ref" )
      val actor = ref.underlyingActor

      ref !+ "FORWARD"
      val snapshot = actor.snapshot

      target.expectMsgPF( max = d, hint = "snapshot" ) {
        case Envelope( "REPLY", h ) => {
          snapshot.envelopeProperties shouldBe props
          snapshot.workId shouldBe h.workId
          snapshot.messageNumber shouldBe MessageNumber( 0L )
          snapshot.myComponentPath shouldBe h.fromComponentPath
          snapshot.myComponentType shouldBe h.fromComponentType
          snapshot.requestId shouldBe RequestId( h.workId, MessageNumber( 0L ) )

          snapshot.envelopeHeader.value.fromComponentPath shouldBe ComponentPath.noSender
          snapshot.envelopeHeader.value.fromComponentType shouldBe ComponentType.noSender
          snapshot.envelopeHeader.value.toComponentPath shouldBe ComponentPath( ref.path )
          snapshot.envelopeHeader.value.messageType shouldBe MessageType( "String" )
          snapshot.envelopeHeader.value.workId shouldBe h.workId
          snapshot.envelopeHeader.value.version shouldBe EnvelopeVersion( 1 )
          snapshot.envelopeHeader.value.properties shouldBe props
        }
      }
    }
  }

//
//  // test( "An ActorStack request should pass messages not understood to unhandled" ) {
//  //   probe.send( source, "MYSTERY" )
//  //   target expectMsg TestActor.Unhandled( "MYSTERY" )
//  // }
}
