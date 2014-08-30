package peds.akka.persistence

import akka.actor.Actor.Receive
import akka.persistence.PersistentActor


trait PersistentActorStack extends PersistentActor {
  def wrappedReceiveCommand: Receive
  def wrappedReceiveRecover: Receive

  override def receiveCommand: Receive = {
    case c if wrappedReceiveCommand.isDefinedAt( c ) => wrappedReceiveCommand( c )
    case c => unhandled( c )
  }

  override def receiveRecover: Receive = {
    case e if wrappedReceiveRecover.isDefinedAt( e ) => wrappedReceiveRecover( e )
    case e => unhandled( e )
  }
}
