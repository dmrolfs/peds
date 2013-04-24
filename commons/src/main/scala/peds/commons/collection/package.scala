package peds.commons

package object collection {
  import scala.language.implicitConversions
  
  implicit def string2StringWithSimilarity( str: String ) = new StringWithSimilarity( str )
}