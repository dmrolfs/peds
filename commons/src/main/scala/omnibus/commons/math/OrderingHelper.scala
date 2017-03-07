package omnibus.commons.math

import scala.math.{Ordered, Ordering}


object OrderingHelper {
  def makeOptionOrdering[T <: Ordered[T]]: Ordering[Option[T]] = new Ordering[Option[T]] {
    def compare( lhs: Option[T], rhs: Option[T] ) = doCompare( lhs, rhs ){ _ compareTo _ }
  }

  def makeOptionOrdering[T]( implicit ord: Ordering[T] ): Ordering[Option[T]] = new Ordering[Option[T]] {
    def compare( lhs: Option[T], rhs: Option[T] ) = doCompare( lhs, rhs ){ ord.compare( _, _ ) }
  }
  // def makeOptionComparator[T <: Ordered[T]]( compareComplete: (T, T) => Int ): Ordering[Option[T]] = new Ordering[Option[T]] {
  //   def compare( lhs: Option[T], rhs: Option[T] ) = doCompare( lhs, rhs )( compareComplete )
  // }

  private def doCompare[T]( lhs: Option[T], rhs: Option[T] )( compare: (T, T) => Int ): Int = {
    if ( compareIncomplete.isDefinedAt( (lhs, rhs) ) ) compareIncomplete( (lhs, rhs) )
    else compare( lhs.get, rhs.get )
  }

  private def compareIncomplete[T]: PartialFunction[(Option[T], Option[T]), Int] = {
    case (Some(_), None) => -1
    case (None, Some(_)) => 1
    case (None,None) => 0
  }
}
