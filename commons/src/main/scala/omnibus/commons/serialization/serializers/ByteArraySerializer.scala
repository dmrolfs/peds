package omnibus.commons.serialization.serializers

import java.io.NotSerializableException
import java.nio.ByteBuffer
import com.typesafe.config.Config
import omnibus.commons.serialization.BaseSerializer

/**
  * This is a special Serializer that Serializes and deserializes byte arrays only,
  * (just returns the byte array unchanged/uncopied)
  *
  * forked from akka.serialization.ByteArraySerializer.
  */
class ByteArraySerializer( override val config: Config )
    extends BaseSerializer
    with ByteBufferSerializer {

  def includeManifest: Boolean = false

  def toBinary( o: AnyRef ): Array[Byte] = o match {
    case null ⇒ null
    case o: Array[Byte] ⇒ o
    case other ⇒
      throw new IllegalArgumentException(
        s"${getClass.getName} only serializes byte arrays, not [${other.getClass.getName}]"
      )
  }

  @throws( classOf[NotSerializableException] )
  override def fromBinary( bytes: Array[Byte], clazz: Option[Class[_]] ): AnyRef = bytes

  override def toBinary( o: AnyRef, buf: ByteBuffer ): Unit = {
    o match {
      case null               => ()
      case bytes: Array[Byte] => buf.put( bytes ); ()
      case other => {
        throw new IllegalArgumentException(
          s"${this.getClass.getName} only serializes byte arrays, not [${other.getClass.getName}]"
        )
      }
    }
  }

  @throws( classOf[NotSerializableException] )
  override def fromBinary( buf: ByteBuffer, manifest: String ): AnyRef = {
    val bytes = new Array[Byte]( buf.remaining() )
    buf.get( bytes )
    bytes
  }
}
