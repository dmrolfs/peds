package peds.commons

import akka.event.LoggingAdapter
import com.typesafe.scalalogging.slf4j.{Logger => TypesafeLogger}

// import grizzled.slf4j.{Logger => GrizzledLogger}


package object log {

  implicit val typesafeTraceable = new Traceable[TypesafeLogger] {
    override def isEnabled( logger: TypesafeLogger ): Boolean = true
    override def trace( logger: TypesafeLogger, msg: ⇒ Any, t: ⇒ Throwable ): Unit = logger.debug( msg.toString, t )
    override def trace( logger: TypesafeLogger, msg: ⇒ Any ): Unit = logger debug msg.toString
  }


  // implicit val grizzledTraceable = new Traceable[GrizzledLogger] {
  //   override def isEnabled( logger: GrizzledLogger ): Boolean = logger.isDebugEnabled
  //   override def trace( logger: GrizzledLogger, msg: ⇒ Any, t: ⇒ Throwable ): Unit = logger.debug( msg, t )
  //   override def trace( logger: GrizzledLogger, msg: ⇒ Any ): Unit = logger debug msg
  // }


  implicit val akkaTraceable = new Traceable[LoggingAdapter] {
    override def isEnabled( logger: LoggingAdapter ): Boolean = logger.isDebugEnabled
    override def trace( logger: LoggingAdapter, msg: ⇒ Any, t: ⇒ Throwable ): Unit = logger.debug( msg.toString, t )
    override def trace( logger: LoggingAdapter, msg: ⇒ Any ): Unit = logger debug msg.toString
  }

}
