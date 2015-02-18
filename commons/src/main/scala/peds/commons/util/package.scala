package peds.commons

import scala.concurrent.{ ExecutionContext, Future }


package object util {

  /**
   * Transforms a function into one that accepts Future arguments and outputs Future results. Used in function composition that pertains
   * to concurrent operations.
   * TODO: MOVE INTO PEDS.
   */
  def futureT[A, B]( f: A => B )( implicit ec: ExecutionContext ): Future[A] => Future[B] = ( a: Future[A] ) => a map { f }

  def optionT[A, B]( f: A => B ): Option[A] => Option[B] = ( a: Option[A] ) => a map { f }

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
    override def apply( x: A ): B = throw new UnsupportedOperationException( "Empty behavior apply()" )
  }

}
