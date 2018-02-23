package omnibus

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import cats.data.{Kleisli, NonEmptyList, ValidatedNel}
import cats.syntax.either._
import journal._


package object commons {
  private val log = Logger[this.type]

  type EC[_] = ExecutionContext

  type AllIssuesOr[A] = ValidatedNel[Throwable, A]
  implicit class ExtractableIssues[A]( val underlying: AllIssuesOr[A] ) extends AnyVal {
    def unsafeGet: A = {
      underlying valueOr { exs =>
        exs map { ex => log.error( s"issue identified extracting validated value:[${underlying}]", ex ) }
        throw exs.head
      }
    }

    def unsafeToErrorOr: ErrorOr[A] = {
      underlying
      .toEither
      .leftMap { exs =>
        exs map { ex => log.error( s"error raised extracting value:[${underlying}]", ex ) }
        exs.head
      }
    }
  }

  type AllErrorsOr[T] = Either[NonEmptyList[Throwable], T]
  implicit class ExtractableErrors[A]( val underlying: AllErrorsOr[A] ) extends AnyVal {
    def unsafeGet: A = {
      underlying valueOr { exs =>
        exs map { ex => log.error( s"error raised extracting V value:[${underlying}]", ex ) }
        throw exs.head
      }
    }

    def unsafeToErrorOr: ErrorOr[A] = {
      underlying leftMap { exs =>
        exs map { ex => log.error( s"error raised extracting value:[${underlying}]", ex ) }
        exs.head
      }
    }
  }

  type ErrorOr[T] = Either[Throwable, T]
  implicit class ExtractableError[A]( val underlying: ErrorOr[A] ) extends AnyVal {
    def unsafeGet: A = {
      underlying valueOr { ex =>
        log.error( s"error raised extracting TryV value:[${underlying}]", ex )
        throw ex
      }
    }
  }

  type KOp[I, O] = Kleisli[ErrorOr, I, O]


  def flatten[T]( xs: Seq[Try[T]] ): Try[Seq[T]] = {
    val ( ss: Seq[Success[T]]@unchecked, fs: Seq[Failure[T]]@unchecked ) = xs.partition(_.isSuccess)

    if ( fs.isEmpty ) Success( ss map { _.get } )
    else Failure[Seq[T]]( fs( 0 ).exception ) // Only keep the first failure
  }
}
