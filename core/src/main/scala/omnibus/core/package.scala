package omnibus

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }
import cats.data.{ Kleisli, NonEmptyList, ValidatedNel }
import omnibus.core.syntax.AllSyntax

package object core extends AllSyntax {
  type AllIssuesOr[A] = ValidatedNel[Throwable, A]

  type AllErrorsOr[T] = Either[NonEmptyList[Throwable], T]

  type ErrorOr[T] = Either[Throwable, T]

  /**
    * makes an emptyBehavior PartialFunction expression that matches no messages at all, ever.
    */
  def emptyBehavior[A, B](): PartialFunction[A, B] = new PartialFunction[A, B] {
    override def isDefinedAt( x: A ): Boolean = false
    override def apply( x: A ): B =
      throw new UnsupportedOperationException( "Empty behavior apply()" )
  }

  type EC[_] = ExecutionContext

  type KOp[I, O] = Kleisli[ErrorOr, I, O]

  def flatten[T]( xs: Seq[Try[T]] ): Try[Seq[T]] = {
    val ( ss: Seq[Success[T]] @unchecked, fs: Seq[Failure[T]] @unchecked ) =
      xs.partition( _.isSuccess )

    if (fs.isEmpty) Success( ss map { _.get } )
    else Failure[Seq[T]]( fs( 0 ).exception ) // Only keep the first failure
  }
}
