package omnibus.commons.partial

import scala.annotation.implicitNotFound

@implicitNotFound( msg = "Cannot find Reducable type class for ${A}" )
trait Reducable[A] {
  def elide( data: A, spec: PartialCriteria ): A
}
