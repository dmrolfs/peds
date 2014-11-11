package peds.akka.supervision

import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import akka.event.LoggingReceive


object IsolatedLifeCycleSupervisor {
  sealed trait Command
  case object WaitForStart extends Command
  case class StartChild( props: Props, name: String ) extends Command

  sealed trait Event
  case object Started extends Event
  case class ChildStarted( child: ActorRef ) extends Event
}

trait IsolatedLifeCycleSupervisor extends Actor with ActorLogging {
  import IsolatedLifeCycleSupervisor._

  /** how to start children during preStart() to be implemented by subclasses */
  def childStarter(): Unit

  final override def preStart(): Unit = childStarter()
  final override def preRestart( reason: Throwable, message: Option[Any] ): Unit = { }
  final override def postRestart( reason: Throwable ): Unit = { }


  override def receive: Receive = LoggingReceive {
    case WaitForStart => sender() ! Started // signify that we've started

    case StartChild( props, name ) => {
      val child = context.actorOf( props, name )
      log info s"started child: ${child}"
      sender() ! ChildStarted( child )
    }

    case m => throw new Exception( s"Don't call ${self.path.name} directly ($m)" )
  }
}
