package omnibus.akka.persistence

import scala.concurrent.duration._
import scala.reflect.ClassTag
import akka.actor.{ Actor, ActorLogging, Cancellable, ReceiveTimeout, NotInfluenceReceiveTimeout }
import akka.persistence.{ SaveSnapshotSuccess, SaveSnapshotFailure, DeleteMessagesSuccess, DeleteMessagesFailure }
import omnibus.akka.ActorStack
import omnibus.akka.envelope.Envelope


trait Passivating extends Actor with ActorStack { outer: Actor with ActorLogging =>
  val StopMessageType: ClassTag[_]

  context.setReceiveTimeout( Duration.Undefined )

  override def postStop(): Unit = {
    // log.debug( "[{}] clearing passivation inactivity timer on actor stop", self.path )
    clearInactivityTimeout()
    super.postStop()
  }

  private var _inactivity: Option[(Cancellable, FiniteDuration, Any)] = None

  def clearInactivityTimeout(): Option[(FiniteDuration, Any)] = {
    val remainder = _inactivity map { case (c, d, m) => c.cancel(); (d, m) }
    _inactivity = None
    // log.debug( "#TEST [{}] Passivating-stack clearInactivityTimeout: remainder-(inactivty, message):[{}]", self.path, remainder )
    remainder
  }

  def setInactivityTimeout( inactivity: Duration, message: Any = ReceiveTimeout ): Option[Cancellable] = {
    // log.debug( "#TEST [{}] Passivating-stack setInactivityTimeout: inactivty:[{}] inactivity-message:[{}]", self.path, inactivity.toCoarsest, message )
    clearInactivityTimeout()
    if ( !inactivity.isFinite() ) None
    else {
      val f = FiniteDuration( inactivity.toNanos, NANOSECONDS )
      // log.debug( "#TEST [{}]:[{}] scheduling to send myself passivating stop message:[{}] in duration:[{}]", outer.getClass.getName, self.path, message, f.toCoarsest )
      val c = context.system.scheduler.scheduleOnce( f, self, message )( context.dispatcher, self )
      _inactivity = Some( (c, f, message) )
      Some( c )
    }
  }

  def touch(): Option[Cancellable] = clearInactivityTimeout() flatMap { case (d, m) => setInactivityTimeout( d, m ) }

  def hasNoInfluence( message: Any): Boolean = { 
    message match {
      case m: NotInfluenceReceiveTimeout => true
      case m: SaveSnapshotSuccess => true
      case m: SaveSnapshotFailure => true
      case m: DeleteMessagesSuccess => true
      case m: DeleteMessagesFailure => true
      case StopMessageType( m ) => true
      case Envelope( payload, _ ) => hasNoInfluence( payload )
      case m => false
    }
  }

  override def around( r: Receive ): Receive = {
    case m if hasNoInfluence( m ) => {
      // log.debug( "[{}] without influence touched by [{}]", self.path, m )
      super.around( r )( m )
    }

    case m => {
      super.around( r )( m )
      // log.debug( "[{}] touched by [{}]", self.path, m )
      touch()
    }
  }
}