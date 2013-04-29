package peds.commons

import scala.annotation.implicitNotFound


package object partial extends XmlReducable with JsonReducable {
  implicit val xmlTransformable = XmlReducable.XmlTransformable
  implicit val jsonTransformable = JsonReducable.JsonTransformable


  @implicitNotFound( msg = "Cannot find Reducable type class for ${A}" )
  trait Reducable[A] {
    def elide( data: A, spec: ElisionCriteria ): A
  }


  def elide[A: Reducable]( data: A, spec: String ): A = {
    implicitly[Reducable[A]].elide( data, (new ElisionParser).parse( spec ) )
  }
}