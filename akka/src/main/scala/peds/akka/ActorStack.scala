package peds.akka

import akka.actor.Actor


trait ActorStack extends Actor {
  def around( r: Receive ): Receive = {
    case m if r.isDefinedAt( m ) => r( m )
    case m => unhandled( m )
  }
}
