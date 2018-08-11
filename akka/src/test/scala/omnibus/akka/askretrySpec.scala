package omnibus.akka

import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import org.scalatest.{ BeforeAndAfterAll, FunSuiteLike, Matchers }
import omnibus.akka.AskRetry._

import scala.concurrent.duration._
import akka.pattern.pipe
import akka.util.Timeout
import akka.actor.Status.Failure
import scribe.Level

class AskRetryTestActor( target: ActorRef ) extends Actor {
  implicit val ec = context.system.dispatcher

  def receive: Actor.Receive = {
    case "ASK" => target.askretry( "MSG", 5, 200.millis ).pipeTo( sender )
  }
}

class AskRetrySuite( _system: ActorSystem )
    extends TestKit( _system )
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  def this() = this( ActorSystem( "AskRetrySuite" ) )

  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  implicit val t = Timeout( 500.millis )
  implicit val ec = system.dispatcher

  val target = TestProbe()
  val source = system.actorOf( Props( classOf[AskRetryTestActor], target.ref ) )
  val probe = TestProbe()

  override def afterAll: Unit = system.terminate()

  test(
    "An ask-retry request should be served correctly if the messages are delivered at the first attempt"
  ) {
    probe.send( source, "ASK" )
    target.expectMsg( "MSG" )
    target.reply( "OK" )
    probe.expectMsg( 100.millis, "OK" )
    probe.expectNoMessage( 500.millis )
  }

  test( "An ask-retry request should retry the specified number of times before failing" ) {
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

  test( "An ask-retry request should retry the specified number of times and then fail" ) {
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
