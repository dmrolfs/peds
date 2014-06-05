package peds.commons


package object util {
  implicit class SimpleClassNameExposure( val underlying: Class[_] ) extends AnyVal {
    def safeSimpleName: String = {
      underlying.getName.split( '.' ).last.split( '$' ).last
    }
  }
}
