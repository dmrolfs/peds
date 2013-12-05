package peds

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import org.joda.{time => joda}


package object commons {
  trait Clock extends ( () => joda.DateTime )

  case object SimpleClock extends Clock {
    override def apply(): joda.DateTime = joda.DateTime.now
  }

  def flatten[T]( xs: Seq[Try[T]] ): Try[Seq[T]] = {
    val ( ss: Seq[Success[T]]@unchecked, fs: Seq[Failure[T]]@unchecked ) = xs.partition(_.isSuccess)

    if ( fs.isEmpty ) Success( ss map { _.get } )
    else Failure[Seq[T]]( fs( 0 ).exception ) // Only keep the first failure
  }

  def tryToFuture[T]( t: => Try[T] ): Future[T] = {
    t match{
      case Success(s) => Future successful { s }
      case Failure(ex) => Future failed { ex }
    }
  }
}
