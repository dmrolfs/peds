package omnibus.commons

import akka.event.LoggingAdapter
import journal._

package object log {

  implicit val typesafeTraceable = new Traceable[Logger] {
    override def isEnabled( logger: Logger ): Boolean = logger.backend.isTraceEnabled
    override def trace( logger: Logger, msg: ⇒ Any, t: ⇒ Throwable ): Unit = {
      logger.debug( msg.toString, t )
    }

    override def trace( logger: Logger, msg: ⇒ Any ): Unit = logger.debug( msg.toString )
  }

  implicit val akkaTraceable = new Traceable[LoggingAdapter] {
    override def isEnabled( logger: LoggingAdapter ): Boolean = logger.isDebugEnabled
    override def trace( logger: LoggingAdapter, msg: ⇒ Any, t: ⇒ Throwable ): Unit =
      logger.debug( msg.toString, t )
    override def trace( logger: LoggingAdapter, msg: ⇒ Any ): Unit = logger debug msg.toString
  }

}
