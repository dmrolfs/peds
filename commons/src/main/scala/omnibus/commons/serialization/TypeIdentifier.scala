package omnibus.commons.serialization

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader


final case class TypeIdentifier( id: Int )

object TypeIdentifier {
  val Undefined = TypeIdentifier( 0 )

  implicit val identifierReader: ValueReader[TypeIdentifier] = new ValueReader[TypeIdentifier] {
    override def read( config: Config, path: String ): TypeIdentifier = TypeIdentifier( id = config.as[Int](path) )
  }
}