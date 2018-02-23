package omnibus.commons.io


/**
 * This is a special Serializer that Serializes and deserializes nulls only
 */
@deprecated( "use serialization package", "0.63" )
class NullBytesSerializer extends Serializer[AnyRef, Array[Byte]] with Deserializer[Array[Byte], AnyRef] {
  val nullAsBytes = Array[Byte]()
  def serialize( o: AnyRef )( implicit c: ConvertFrom[AnyRef, Array[Byte]] ): Either[Throwable, Array[Byte]] = Right( nullAsBytes )
  def deserialize( bytes: Array[Byte] )( implicit c: ConvertFrom[Array[Byte], AnyRef] ): Either[Throwable, AnyRef] = Right( null )
}

object NullBytesSerializer extends NullBytesSerializer