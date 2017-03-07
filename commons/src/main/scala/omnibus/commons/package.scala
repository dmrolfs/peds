package omnibus

import scala.util.{ Try, Success, Failure }
import scalaz.{ Kleisli, NonEmptyList, ValidationNel, \/, \/-, -\/, Success => ZSuccess, Failure => ZFailure }
import com.typesafe.scalalogging.LazyLogging


package object commons extends LazyLogging {
  type Valid[A] = ValidationNel[Throwable, A] //todo: why did scalaz get rid of error's covariance from 7.1.x to 7.2?
  object Valid {
    def unsafeGet[A]( va: Valid[A] ): A = {
      va match {
        case ZSuccess( a ) => a
        case ZFailure( exs ) => {
          exs foreach { ex => logger.error( s"error surfaced extracting validated value:[${va}]", ex ) }
          throw exs.head
        }
      }
    }
  }

  type V[T] = \/[NonEmptyList[Throwable], T]
  object V {
    def unsafeGet[T]( vt: V[T] ): T = {
      vt match {
        case \/-( v ) => v
        case -\/( exs ) => {
          exs foreach { ex => logger.error( s"error surfaced extracting V value:[${vt}]", ex ) }
          throw exs.head
        }
      }
    }
  }

  type TryV[T] = Throwable \/ T
  object TryV {
    def unsafeGet[T]( vt: TryV[T] ): T = {
      vt match {
        case \/-( v ) => v
        case -\/( ex ) => {
          logger.error( s"error surfaced extracting TryV value:[${vt}]", ex )
          throw ex
        }
      }
    }
  }

  type KOp[I, O] = Kleisli[TryV, I, O]


  def flatten[T]( xs: Seq[Try[T]] ): Try[Seq[T]] = {
    val ( ss: Seq[Success[T]]@unchecked, fs: Seq[Failure[T]]@unchecked ) = xs.partition(_.isSuccess)

    if ( fs.isEmpty ) Success( ss map { _.get } )
    else Failure[Seq[T]]( fs( 0 ).exception ) // Only keep the first failure
  }
}
