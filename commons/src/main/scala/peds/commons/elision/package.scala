package peds.commons


package object elision extends XmlReducable with JsonReducable {
  implicit val xmlTransformable = XmlReducable.XmlTransformable
  implicit val jsonTransformable = JsonReducable.JsonTransformable
}