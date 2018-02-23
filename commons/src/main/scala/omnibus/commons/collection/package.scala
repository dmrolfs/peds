package omnibus.commons

package object collection {
  import scala.language.implicitConversions

  @deprecated( "use rockymadden/stringmetric library instead", "0.61" )
  implicit def string2StringWithSimilarity( str: String ): StringWithSimilarity = new StringWithSimilarity( str )
}