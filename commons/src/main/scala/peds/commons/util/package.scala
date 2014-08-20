package peds.commons


package object util {
  implicit class SimpleClassNameExposure( val underlying: Class[_] ) extends AnyVal {
    def safeSimpleName: String = {
      underlying.getName.split( '.' ).last.split( '$' ).last
    }
  }


  /**
   * makes an emptyBehavior PartialFunction expression that matches no messages at all, ever.
   */
  def emptyBehavior[A, B](): PartialFunction[A, B] = new PartialFunction[A, B] {
    override def isDefinedAt( x: A ): Boolean = false
    override def apply( x: A ): B = throw new UnsupportedOperationException( "Empty behavior apply()" )
  }

}
