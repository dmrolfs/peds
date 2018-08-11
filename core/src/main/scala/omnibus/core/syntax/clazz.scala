package omnibus.core.syntax

import scala.language.implicitConversions

trait ClassSyntax {
  implicit final def safeSimpleClassnameExposure( c: Class[_] ): SafeSimpleName = {
    new SafeSimpleName( c )
  }
}

final class SafeSimpleName( val underlying: Class[_] ) extends AnyVal {

  def safeSimpleName: String = {
    underlying.getName.split( '.' ).last.split( '$' ).last
  }
}
