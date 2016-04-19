package peds

import scala.util._
import scalaz.{Kleisli, NonEmptyList, ValidationNel, \/}


package object commons {
  type Valid[A] = ValidationNel[Throwable, A] //todo: why did scalaz get rid of error's covariance from 7.1.x to 7.2?
  type V[T] = \/[NonEmptyList[Throwable], T]

  type TryV[T] = Throwable\/T

  type KOp[I, O] = Kleisli[TryV, I, O]


  def flatten[T]( xs: Seq[Try[T]] ): Try[Seq[T]] = {
    val ( ss: Seq[Success[T]]@unchecked, fs: Seq[Failure[T]]@unchecked ) = xs.partition(_.isSuccess)

    if ( fs.isEmpty ) Success( ss map { _.get } )
    else Failure[Seq[T]]( fs( 0 ).exception ) // Only keep the first failure
  }
}
