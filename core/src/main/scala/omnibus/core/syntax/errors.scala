package omnibus.core.syntax

import scala.language.implicitConversions
import cats.syntax.either._
import journal._
import omnibus.core.{ AllErrorsOr, AllIssuesOr, ErrorOr }

trait ErrorsSyntax {
  implicit def extractableIssues[A]( issues: AllIssuesOr[A] ): ExtractableIssues[A] = {
    new ExtractableIssues( issues )
  }

}

final class ExtractableIssues[A]( val underlying: AllIssuesOr[A] ) extends AnyVal {
  def log: Logger = Logger( "ExtractableIssues" )

  def unsafeGet: A = {
    underlying valueOr { exs =>
      exs map { ex =>
        log.error( s"issue identified extracting validated value:[${underlying}]", ex )
      }
      throw exs.head
    }
  }

  def unsafeToErrorOr: ErrorOr[A] = {
    underlying.toEither
      .leftMap { exs =>
        exs map { ex =>
          log.error( s"error raised extracting value:[${underlying}]", ex )
        }
        exs.head
      }
  }
}

final class ExtractableErrors[A]( val underlying: AllErrorsOr[A] ) extends AnyVal {
  def log: Logger = Logger( "ExtractableErrors" )

  def unsafeGet: A = {
    underlying valueOr { exs =>
      exs map { ex =>
        log.error( s"error raised extracting V value:[${underlying}]", ex )
      }
      throw exs.head
    }
  }

  def unsafeToErrorOr: ErrorOr[A] = {
    underlying leftMap { exs =>
      exs map { ex =>
        log.error( s"error raised extracting value:[${underlying}]", ex )
      }
      exs.head
    }
  }
}

final class ExtractableError[A]( val underlying: ErrorOr[A] ) extends AnyVal {
  def log: Logger = Logger( "ExtractableError" )

  def unsafeGet: A = {
    underlying valueOr { ex =>
      log.error( s"error raised extracting TryV value:[${underlying}]", ex )
      throw ex
    }
  }
}
