package peds.commons.util


/**
 * The ThreadLocal gets initialized by the init that is passed in when needed.  
 * And, because it just extends java.lang.ThreadLocal, you can also call set on it too. 
 *
 * Usage:
 * val tl = new ThreadLocal(System.currentTimeMillis)
 *
 * // these three are all equivalent, and depends on your taste
 * tl.withValue { v => System.out.println(v) }
 * System.out.println(tl.get)
 * System.out.println(tl())
 */
class ThreadLocal[T]( init: => T ) extends java.lang.ThreadLocal[T] with Function0[T] {
  override def initialValue: T = init

  def apply = get

  def withValue[S]( thunk: (T => S) ):S = thunk( get )
}