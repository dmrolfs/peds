package omnibus.commons

import akka.event.LoggingAdapter
import scribe.{ Level, Logger }

package object log {

//  @deprecated( "use scribe trace", "0.63" )
  implicit val typesafeTraceable = new Traceable[Logger] {
    override def isEnabled( logger: Logger ): Boolean = logger includes Level.Trace
    override def trace( logger: Logger, msg: ⇒ Any, t: ⇒ Throwable ): Unit =
      logger.trace( msg.toString, t )
    override def trace( logger: Logger, msg: ⇒ Any ): Unit = logger trace msg.toString
  }

//  @deprecated( "use scribe trace", "0.63" )
  implicit val akkaTraceable = new Traceable[LoggingAdapter] {
    override def isEnabled( logger: LoggingAdapter ): Boolean = logger.isDebugEnabled
    override def trace( logger: LoggingAdapter, msg: ⇒ Any, t: ⇒ Throwable ): Unit =
      logger.debug( msg.toString, t )
    override def trace( logger: LoggingAdapter, msg: ⇒ Any ): Unit = logger debug msg.toString
  }

}
