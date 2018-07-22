package omnibus.commons

import akka.event.LoggingAdapter
import journal.{ Logger => JournalLogger }

package object log {

//  @deprecated( "use journal debug", "0.63" )
  implicit val typesafeTraceable = new Traceable[JournalLogger] {
    override def isEnabled( logger: JournalLogger ): Boolean = logger.backend.isDebugEnabled
    override def trace( logger: JournalLogger, msg: ⇒ Any, t: ⇒ Throwable ): Unit =
      logger.debug( msg.toString, t )
    override def trace( logger: JournalLogger, msg: ⇒ Any ): Unit = logger debug msg.toString
  }

//  @deprecated( "use journal debug", "0.63" )
  implicit val akkaTraceable = new Traceable[LoggingAdapter] {
    override def isEnabled( logger: LoggingAdapter ): Boolean = logger.isDebugEnabled
    override def trace( logger: LoggingAdapter, msg: ⇒ Any, t: ⇒ Throwable ): Unit =
      logger.debug( msg.toString, t )
    override def trace( logger: LoggingAdapter, msg: ⇒ Any ): Unit = logger debug msg.toString
  }

}
