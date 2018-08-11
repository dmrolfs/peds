package omnibus.akka.publish

import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import org.scalatest.{ BeforeAndAfterAll, FunSuiteLike, Matchers }

import scala.concurrent.duration._
import akka.util.Timeout
import omnibus.akka.ActorStack
import scribe.Level

object TestActorStack {
  case class Unhandled( message: Any )
}

class TestActorStack( target: ActorRef ) extends Actor with ActorStack {
  implicit val ec = context.system.dispatcher

  def receive: Actor.Receive = around( { case "ASK" => target ! "REPLY" } )

  override def unhandled( message: Any ): Unit = {
    target ! TestActorStack.Unhandled( message )
  }
}

class ActorStackSpec( _system: ActorSystem )
    extends TestKit( _system )
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {
  def this() = this( ActorSystem( "ActorStackSpec" ) )

  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  implicit val t = Timeout( 500.millis )
  implicit val ec = system.dispatcher

  val target = TestProbe()
  val source = system.actorOf( Props( classOf[TestActorStack], target.ref ) )
  val probe = TestProbe()

  override def afterAll: Unit = system.terminate()

  test( "An ActorStack request should pass through messages understood by the actor" ) {
    probe.send( source, "ASK" )
    target expectMsg "REPLY"
  }

  test( "An ActorStack request should pass messages not understood to unhandled" ) {
    probe.send( source, "MYSTERY" )
    target expectMsg TestActorStack.Unhandled( "MYSTERY" )
  }
}
