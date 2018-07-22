package omnibus.commons.io

import scala.xml.Node
import omnibus.commons.util.NonFatal

/** Used to serialize objects into XML.
  *
  * example:
  * <code>def output[T]( what: T )( implicit c: ConvertFrom[T, Node] ) = new XmlSerializer[T]{}.serialize(what)</code>
  */
@deprecated( "use serialization package", "0.63" )
trait XmlSerializer[T] extends Serializer[T, Node] {

  /**
    * Serializes the given object into XML
    */
  def serialize( o: T )( implicit c: ConvertFrom[T, Node] ): Either[Throwable, Node] = {
    try { Right( trim( c( o ) ) ) } catch { case NonFatal( e ) => Left( e ) }
  }

  private def trim( x: Node ): Node = scala.xml.Utility.trim( x )
}

object XmlSerializer {

  def toXml[T]( what: T )( implicit c: ConvertFrom[T, Node] ) =
    new XmlSerializer[T] {}.serialize( what )

  def forTraversable[T](
    elem: String
  )( t: Traversable[T] )( implicit c: ConvertFrom[T, Node] ): Either[Throwable, Node] = {
    try {
      Right( <List>{ t.map{ c( _ ) } }</List>.copy( label = elem ) )
    } catch {
      case NonFatal( e ) => Left( e )
    }
  }

  def forOption[T](
    elem: String
  )( o: Option[T] )( implicit c: ConvertFrom[T, Node] ): Either[Throwable, Node] = {
    try {
      if (!o.isEmpty) Right( <ELEM>{ c( o.get ) }</ELEM>.copy( label = elem ) )
      else Right( <ELEM/>.copy( label = elem ) )
    } catch {
      case NonFatal( e ) => Left( e )
    }
  }
}
