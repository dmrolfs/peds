package peds.akka.supervision

import akka.actor.{ Actor, ActorRef, Props, ActorLogging, Terminated, DeathPactException }


object IsolatedLifeCycleSupervisor {
  sealed trait Command
  case object WaitForStart extends Command
  case class StartChild( props: Props, name: String ) extends Command
  case object GetChildren extends Command

  sealed trait Event
  case object Started extends Event
  case class Children( children: Iterable[ChildStarted] ) extends Event
  case class ChildStarted( child: ActorRef ) extends Event {
    def name: String = child.path.name
  }
}

trait IsolatedLifeCycleSupervisor extends Actor with ActorLogging {
  import IsolatedLifeCycleSupervisor._

  /** how to start children during preStart() to be implemented by subclasses */
  def childStarter(): Unit

  final override def preStart(): Unit = childStarter()
  final override def preRestart( reason: Throwable, message: Option[Any] ): Unit = { }
  final override def postRestart( reason: Throwable ): Unit = { }


  override def receive: Receive = {
    case WaitForStart => sender() ! Started // signify that we've started

    case GetChildren => sender() ! Children( context.children map { ChildStarted } )

    case StartChild( props, name ) => {
      val child = context.actorOf( props, name )
      log info s"started child: ${child}"
      sender() ! ChildStarted( child )
    }
  }

  override def unhandled( m: Any ): Unit = throw new IllegalStateException( s"Don't call ${self.path.name} directly ($m)" )
}
