package omnibus.commons

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

package object util {

  /**
    * Transforms a function into one that accepts Future arguments and outputs Future results. Used in function composition that pertains
    * to concurrent operations.
    * TODO: MOVE INTO omnibus.
    */
  def futureT[A, B]( f: A => B )( implicit ec: ExecutionContext ): Future[A] => Future[B] =
    (a: Future[A]) => a map { f }

  def optionT[A, B]( f: A => B ): Option[A] => Option[B] = (a: Option[A]) => a map { f }

  // def castT[I : ClassTag, O : ClassTag]: I => O = in => {
  //   in.cast[O] getOrElse {
  //     val msg = s"${implicitly[ClassTag[I]].runtimeClass.safeSimpleName} is not ${implicitly[ClassTag[O]].runtimeClass.safeSimpleName}: ${in}"
  //     logger warn msg
  //     throw new ClassCastException( msg )
  //   }
  // }

  def tryToFuture[A]( t: => Try[A] )( implicit ec: ExecutionContext ): Future[A] = {
    Future { t } flatMap {
      case Success( s )    => Future successful s
      case Failure( fail ) => Future failed fail
    }
  }

  implicit class SimpleClassNameExposure( val underlying: Class[_] ) extends AnyVal {

    def safeSimpleName: String = {
      underlying.getName.split( '.' ).last.split( '$' ).last
    }
  }

  /**
    * makes an emptyBehavior PartialFunction expression that matches no messages at all, ever.
    */
  def emptyBehavior[A, B](): PartialFunction[A, B] = new PartialFunction[A, B] {
    override def isDefinedAt( x: A ): Boolean = false
    override def apply( x: A ): B =
      throw new UnsupportedOperationException( "Empty behavior apply()" )
  }

}
