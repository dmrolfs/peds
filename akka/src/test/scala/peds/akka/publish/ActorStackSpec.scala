package peds.akka.publish

import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import akka.actor.{Props, ActorRef, Actor, ActorSystem}
import org.scalatest.{FunSuiteLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import akka.pattern.pipe
import akka.util.Timeout
import akka.actor.Status.Failure
import peds.akka.ActorStack


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
with ImplicitSender
{
  def this() = this( ActorSystem( "ActorStackSpec" ) )

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
