package peds.commons

import scala.annotation.implicitNotFound


/**
 * The partial package is useful in filtering RESTful interfaces in order to focus a response on only the requried data.
 * This has several benefits to the operation of APIs. It reduces the bandwidth required to support an API and thereby 
 * reduces reduces the operational cost for the service. For large scale services this can be significant.
 */
package object partial extends XmlReducable with JsonReducable {
  implicit val xmlTransformable = XmlReducable.XmlTransformable
  implicit val jsonTransformable = JsonReducable.JsonTransformable


  @implicitNotFound( msg = "Cannot find Reducable type class for ${A}" )
  trait Reducable[A] {
    def elide( data: A, spec: PartialCriteria ): A
  }


  def elide[A: Reducable]( data: A, spec: String ): A = {
    implicitly[Reducable[A]].elide( data, (new PartialParser).parse( spec ) )
  }
}