package peds.akka.persistence

import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.actor.{ Actor, ActorLogging, Cancellable, ReceiveTimeout, NotInfluenceReceiveTimeout }
import akka.persistence.{ SaveSnapshotSuccess, SaveSnapshotFailure, DeleteMessagesSuccess, DeleteMessagesFailure }
import peds.akka.ActorStack


trait Passivating extends ActorStack { outer: Actor with ActorLogging =>
  val StopMessageType: ClassTag[_]

  private var _inactivity: Option[(Cancellable, FiniteDuration, Any)] = None

  def clearInactivityTimeout(): Option[(FiniteDuration, Any)] = {
    val remainder = _inactivity map { case (c, d, m) => c.cancel(); (d, m) }
    _inactivity = None
    remainder
  }

  def setInactivityTimeout( inactivity: Duration, message: Any = ReceiveTimeout ): Option[Cancellable] = {
    clearInactivityTimeout()
    if ( !inactivity.isFinite() ) None
    else {
      val f = FiniteDuration( inactivity.toNanos, NANOSECONDS )
      val c = context.system.scheduler.scheduleOnce( f, self, message )( context.dispatcher, self )
      _inactivity = Some( (c, f, message) )
      Some( c )
    }
  }

  def touch(): Option[Cancellable] = clearInactivityTimeout() flatMap { case (d, m) => setInactivityTimeout( d, m ) }

  override def around( r: Receive ): Receive = {
    case m: NotInfluenceReceiveTimeout => super.around( r )( m )
    case m: SaveSnapshotSuccess => super.around( r )( m )
    case m: SaveSnapshotFailure => super.around( r )( m )
    case m: DeleteMessagesSuccess => super.around( r )( m )
    case m: DeleteMessagesFailure => super.around( r )( m )
    case StopMessageType( m ) => super.around( r )( m )
    case m => {
      super.around( r )( m )
      touch()
    }
  }
}