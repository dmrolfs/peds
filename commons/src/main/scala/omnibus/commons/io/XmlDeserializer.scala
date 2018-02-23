package omnibus.commons.io

import scala.xml.Node
import omnibus.commons.util.NonFatal
import omnibus.commons.log.Trace


/** Used to deserialize XML nodes into objects
 * example:
 * <code>def input[T]( src: Node )( implicit c: ConvertFrom[Node, T] ) = new XmlDeserializer[T]{}.deserialize(src)</code>
 */
@deprecated( "use serialization package", "0.63" )
trait XmlDeserializer[T] extends Deserializer[Node, T] {
  /**
   * Produces an object from a Format type, with an optional type-hint;
   * the class should be loaded using ActorSystem.dynamicAccess.
   */
  def deserialize( node: Node )( implicit c: ConvertFrom[Node, T] ): Either[Throwable, T] = {
    try { Right( c(node) ) }
    catch { case  NonFatal( e ) => Left( e ) }
  }
}

object XmlDeserializer {
  val trace = Trace( "XmlDeserializer" )

  def fromXml[T]( n: Node )( implicit c: ConvertFrom[Node, T] ) = new XmlDeserializer[T]{}.deserialize( n )

  def forTraversable[T]( src: Node )( implicit c: ConvertFrom[Node, T] ): Either[Throwable, Traversable[T]] = trace.block("forTraversable") {
    try { 
      Right( 
        src.child map{ e =>
          trace( "child="+e )
          trace.block( "convert" ){ c( e ) }
        } 
      ) 
    } catch { 
      case NonFatal( e ) => Left( e ) 
    }
  }

  def forOption[T]( parent: Node, target: String )( implicit c: ConvertFrom[Node, T] ): Either[Throwable, Option[T]] = {
    try {
      val elem = ( parent \ target )
      if ( elem.isEmpty ) Right( None )
      else Right( Some( c(elem.head) ) )
    } catch {
      case NonFatal( e ) => Left( e )
    }
  }

}