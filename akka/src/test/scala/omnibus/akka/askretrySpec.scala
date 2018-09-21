package omnibus.akka

import akka.testkit.TestProbe
import akka.actor.{ Actor, ActorRef, ActorSystem, PoisonPill, Props }
import org.scalatest.{ Matchers, Tag }
import omnibus.akka.AskRetry._

import scala.concurrent.duration._
import akka.pattern.pipe
import akka.util.Timeout
import akka.actor.Status.Failure
import journal._
import omnibus.akka.testkit.ParallelAkkaSpec

class AskRetryTestActor( target: ActorRef ) extends Actor {
  implicit val ec = context.system.dispatcher

  def receive: Actor.Receive = {
    case "ASK" => target.askretry( "MSG", 5, 200.millis ).pipeTo( sender )
  }
}

class AskRetrySuite extends ParallelAkkaSpec with Matchers {
  private val log = Logger[AskRetrySuite]

  class Fixture( override val slug: String, _system: ActorSystem )
      extends AkkaFixture( slug, _system ) {
    implicit val t = Timeout( 500.millis )
    implicit val ec = system.dispatcher

    val target = TestProbe( "target" )
    val source = system.actorOf( Props( classOf[AskRetryTestActor], target.ref ), "source" )
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

  "Ask-Retry Request" should {
    "An ask-retry request should be served correctly if the messages are delivered at the first attempt" in {
      f: Fixture =>
        import f._
        probe.send( source, "ASK" )
        target.expectMsg( "MSG" )
        target.reply( "OK" )
        probe.expectMsg( 100.millis, "OK" )
        probe.expectNoMessage( 500.millis )
    }

    "An ask-retry request should retry the specified number of times before failing" in {
      f: Fixture =>
        import f._
        probe.send( source, "ASK" )
        probe.expectNoMessage( 800.millis )
        target.expectMsg( "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.reply( "OK" )
        probe.expectMsg( "OK" )
        probe.expectNoMessage( 500.millis )
    }

    "An ask-retry request should retry the specified number of times and then fail" in {
      f: Fixture =>
        import f._
        probe.send( source, "ASK" )
        probe.expectNoMessage( 1000.millis )
        target.expectMsg( "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        target.expectMsg( 200.millis, "MSG" )
        probe.expectMsgClass( 200.millis, classOf[Failure] ).cause.getClass should equal(
          classOf[RetryException]
        )
        target.reply( "OK" )
        probe.expectNoMessage( 500.millis )
    }
  }
}
