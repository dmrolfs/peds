package omnibus.spray


package object akka {
  import scala.language.implicitConversions

  implicit def string2ActorName( s: String ): StringActorName = new StringActorName( s )

  class StringActorName( underlying: String ) {
    import StringActorName._
    def toActorSuitable: String = for ( c <- augmentString(underlying) ) yield { if ( allowed.contains(c) ) c else '_' }
  }

  object StringActorName {
    val allowed: Set[Char] = Set( 
      '-', ':', '@', '&', '=', '+', ',', '.', '!', '~', '*', ''', '$', '_', ';',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )
  }
  
}