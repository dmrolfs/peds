package peds.akka

import akka.actor.Actor
import akka.persistence.PersistentActor


trait ActorStack extends Actor {
  def wrappedReceive: Receive

  override def receive: Receive = {
    case c if wrappedReceive.isDefinedAt( c ) => wrappedReceive( c )
    case c => unhandled( c )
  }
}
