package peds

import scala.concurrent.Future
import scala.util._


package object commons {
  implicit def rillit2Shapeless[A,B]( lenser: rillit.InitializedLenser[A,B] ): shapeless.Lens[A,B] = new shapeless.Lens[A, B] {
    val lens: rillit.Lens[A,B] = lenser()
    override def get( that: A ): B = lens.get( that )
    override def set( that: A )( value: B ): A = lens.set( that, value )
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
