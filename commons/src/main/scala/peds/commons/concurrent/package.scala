package peds.commons

import scala.concurrent.Future
import scala.util._


package object concurrent {
  def tryToFuture[T]( t: => Try[T] ): Future[T] = {
    t match{
      case Success(s) => Future successful { s }
      case Failure(ex) => Future failed { ex }
    }
  }
}
