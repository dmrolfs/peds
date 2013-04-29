// package peds.commons.partial

// import scala.annotation.implicitNotFound


// object Partial {
//   def elide[A: Reducable]( data: A, spec: String ): A = {
//     implicitly[Reducable[A]].elide( data, (new ElisionParser).parse( spec ) )
//   }
// }


// @implicitNotFound( msg = "Cannot find Reducable type class for ${A}" )
// trait Reducable[A] {
//   def elide( data: A, spec: ElisionCriteria ): A
// }