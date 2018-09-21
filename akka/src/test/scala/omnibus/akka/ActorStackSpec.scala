package omnibus.akka

import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, ActorSystem, PoisonPill, Props }
import akka.testkit.TestProbe
import akka.util.Timeout
import omnibus.akka.testkit.ParallelAkkaSpec
import org.scalatest.{ Matchers, Tag }
import journal._

object TestActorStack {
  def props( target: ActorRef ): Props = Props( new TestActorStack( target ) )
  case class Unhandled( message: Any )
}

class TestActorStack( target: ActorRef ) extends Actor with ActorStack {
  implicit val ec = context.system.dispatcher

  def receive: Actor.Receive = around( { case "ASK" => target ! "REPLY" } )

  override def unhandled( message: Any ): Unit = {
    target ! TestActorStack.Unhandled( message )
  }
}

class ActorStackSpec extends ParallelAkkaSpec with Matchers {
  private val log = Logger[ActorStackSpec]

  class Fixture( override val slug: String, _system: ActorSystem )
      extends AkkaFixture( slug, _system ) {
    implicit val t = Timeout( 500.millis )
    implicit val ec = system.dispatcher

    val target = TestProbe( "target" )
    val source = system.actorOf( TestActorStack.props( target.ref ), "source" )
    val probe = TestProbe( "probe" )

    override def after( test: OneArgTest ): Unit = {
      log.debug( s"killing fixture[${slug}]'s source actor:[${source.path}]..." )
      source ! PoisonPill
    }
  }

  override def createAkkaFixture( test: OneArgTest, system: ActorSystem, slug: String ): Fixture = {
    new Fixture( slug, system )
  }

  object WIP extends Tag( "wip" )

  "ActorStack" should {
    "pass through messages understood by the actor" in { f: Fixture =>
      import f._
      probe.send( source, "ASK" )
      target expectMsg "REPLY"
    }

    "pass messages not understood to unhandled" in { f: Fixture =>
      import f._
      probe.send( source, "MYSTERY" )
      target expectMsg TestActorStack.Unhandled( "MYSTERY" )
    }
  }
}
