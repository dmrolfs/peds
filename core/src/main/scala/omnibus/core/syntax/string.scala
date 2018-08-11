package omnibus.core.syntax

import scala.language.implicitConversions

trait StringSyntax {
  implicit final def caseConversionsExposure( s: String ): CaseConversions =
    new CaseConversions( s )
}

final class CaseConversions( val underlying: String ) extends AnyVal {

  def kebabToCamel( key: String ): String = {
    "-([a-z\\d])".r.replaceAllIn( key, { _.group( 1 ).toUpperCase() } )
  }

}
