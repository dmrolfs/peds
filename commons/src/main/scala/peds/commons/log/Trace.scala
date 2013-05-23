package peds.commons.log

import grizzled.slf4j.{Logger => GrizzledLogger}


class Trace( val logger: GrizzledLogger ) {
  
  /** Get the name associated with this logger.
   *
   * @return the name.
   */
  @inline final def name = logger.name
  
  /** Determine whether trace logging is enabled.
   * In the future modify to determine enabled based on logger.name
   */
  @inline final def isEnabled = logger.isTraceEnabled
  
  @inline final def apply( msg: => Any ): Unit = if ( isEnabled ) logger.trace( msg )
  @inline final def apply( msg: => Any, t: => Throwable ): Unit = if ( isEnabled ) logger.trace( msg, t )

  /** Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def msg( msg: => Any ): Unit = if ( isEnabled ) logger.trace( msg )
  
  /** Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def msg( msg: => Any, t: => Throwable ): Unit = if ( isEnabled ) logger.trace( msg, t )

  @inline final def block[T]( label: => Any )( block: => T ): T = Trace.block[T]( name + "." + label )( block )
}


object Trace {

  def apply( logger: GrizzledLogger ): Trace = new Trace( logger )
  
  /** Get the logger with the specified name. Use `RootName` to get the
   * root logger.
   *
   * @param name  the logger name
   *
   * @return the `Trace`.
   */
  def apply( name: String ): Trace = new Trace( GrizzledLogger(name) )

  /** Get the logger for the specified class, using the class's fully
   * qualified name as the logger name.
   *
   * @param cls  the class
   *
   * @return the `Trace`.
   */
  def apply( cls: Class[_] ): Trace = new Trace( GrizzledLogger(cls) )

  /** Get the logger for the specified class type, using the class's fully
   * qualified name as the logger name.
   *
   * @return the `Trace`.
   */
  def apply[C]( implicit m: Manifest[C] ): Trace = new Trace( GrizzledLogger[C] )
  
  import peds.commons.util.ThreadLocal
  import collection._
  private var scopeStack = new ThreadLocal( mutable.Stack[String]() )
  val stackLabel = "fnstack"
  lazy val stackTrace = Trace( stackLabel )
  
  @deprecated( "use Trace.block[T}", "")
  def traceBlock[T]( label: => Any )( block: => T ): T = Trace.block[T]( label )( block )

  def block[T]( label: => Any )( block: => T ): T = {
    if ( stackTrace.isEnabled ) {
      enter( label )
      val result: T = block
      exit( result )
      result
    } else {
      block
    }
  }
  
  def enter( label: String ) {
    if ( stackTrace.isEnabled ) {
      scopeStack withValue { s =>
        stackTrace.logger.trace( " " * (s.length * 2) + "+ %s".format(label) )
        s push label
      }
    }
  }
  
  def exit[T]( extra: T ) {
    if ( stackTrace.isEnabled ) {
      scopeStack withValue { s =>
        val label = s.pop
        var record = " " * ( s.length * 2 ) + "- %s".format( label )

        extra match {
          case _: Unit => 
          case x => record += " = %s".format( x )
        }
        stackTrace.logger.trace( record )
      }
    }
  }


  import scala.language.implicitConversions

  /** Converts any type to a String. In case the object is null, a null 
   * String is returned. Otherwise the method `toString()` is called.
   *
   * @param msg  the message object to be converted to String
   *
   * @return the String representation of the message.
   */
  implicit def _any2String( msg: Any ): String = msg match {
    case null => "<null>"
    case _ => msg.toString
  }
}