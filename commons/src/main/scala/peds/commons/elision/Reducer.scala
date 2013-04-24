package peds.commons.elision

import scala.annotation.implicitNotFound


object Reducer {
  def elide[A: Reducable]( data: A, spec: String ): A = {
    implicitly[Reducable[A]].elide( data, (new ElisionParser).parse( spec ) )
  }
}


@implicitNotFound( msg = "Cannot find Reducable type class for ${A}" )
trait Reducable[A] {
  def elide( data: A, spec: ElisionCriteria ): A
}