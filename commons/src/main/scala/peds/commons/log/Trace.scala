package peds.commons.log

import scala.reflect.ClassTag
import com.typesafe.scalalogging.{Logger => TypesafeLogger}
import peds.commons.util._


class Trace[L: Traceable]( val name: String, logger: L ) {
  
  /** Determine whether trace logging is enabled.
   * In the future modify to determine enabled based on logger.name
   */
  @inline final def isEnabled: Boolean = implicitly[Traceable[L]].isEnabled( logger )
  
  @inline final def apply( msg: => Any ): Unit = if ( isEnabled ) implicitly[Traceable[L]].trace( logger, msg )
  @inline final def apply( msg: => Any, t: => Throwable ): Unit = {
    if ( isEnabled ) implicitly[Traceable[L]].trace( logger, msg, t )
  }

  /** Issue a trace logging message.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   */
  @inline final def msg( msg: => Any ): Unit = apply( msg )
  
  /** Issue a trace logging message, with an exception.
   *
   * @param msg  the message object. `toString()` is called to convert it
   *             to a loggable string.
   * @param t    the exception to include with the logged message.
   */
  @inline final def msg( msg: => Any, t: => Throwable ): Unit = apply( msg, t )

  @inline final def block[R]( label: => Any )( block: => R ): R = Trace.block[R]( name + "." + label )( block )
}


object Trace {
  import org.slf4j.{LoggerFactory => Slf4jLoggerFactory}

  type DefaultLogger = TypesafeLogger

  def apply[L: Traceable]( name: String, logger: L ): Trace[L] = new Trace( name, logger )
  
  /** Get the logger with the specified name. Use `RootName` to get the
   * root logger.
   *
   * @param name  the logger name
   *
   * @return the `Trace`.
   */
  def apply( name: String ): Trace[DefaultLogger] = new Trace( name, TypesafeLogger( Slf4jLoggerFactory getLogger name ) )

  /** Get the logger for the specified class, using the class's fully
   * qualified name as the logger name.
   *
   * @param cls  the class
   *
   * @return the `Trace`.
   */
  def apply( cls: Class[_] ): Trace[DefaultLogger] = {
    new Trace( cls.safeSimpleName, TypesafeLogger( Slf4jLoggerFactory getLogger cls) )
  }

  /** Get the logger for the specified class type, using the class's fully
   * qualified name as the logger name.
   *
   * @return the `Trace`.
   */
  def apply[C: ClassTag](): Trace[DefaultLogger] = {
    val clazz = implicitly[ClassTag[C]].runtimeClass
    new Trace( clazz.safeSimpleName, TypesafeLogger( Slf4jLoggerFactory getLogger clazz ) )
  }

  
  import peds.commons.util.ThreadLocal
  import collection._
  private var scopeStack = new ThreadLocal( mutable.Stack[String]() )
  val stackLabel = "fnstack"
  lazy val stackTrace = Trace( stackLabel )
  
  def block[R]( label: => Any )( block: => R ): R = {
    if ( stackTrace.isEnabled ) {
      enter( label )
      val result: R = block
      exit( result )
      result
    } else {
      block
    }
  }
  
  def enter( label: String ) {
    if ( stackTrace.isEnabled ) {
      scopeStack withValue { s =>
        stackTrace( " " * (s.length * 2) + "+ %s".format(label) )
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
        stackTrace( record )
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