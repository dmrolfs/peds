package peds

import scala.util._


package object commons {
  def flatten[T]( xs: Seq[Try[T]] ): Try[Seq[T]] = {
    val ( ss: Seq[Success[T]]@unchecked, fs: Seq[Failure[T]]@unchecked ) = xs.partition(_.isSuccess)

    if ( fs.isEmpty ) Success( ss map { _.get } )
    else Failure[Seq[T]]( fs( 0 ).exception ) // Only keep the first failure
  }
}
