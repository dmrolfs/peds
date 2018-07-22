package omnibus.commons.serialization.serializers

import java.io.NotSerializableException
import omnibus.commons.serialization.{Serializer, TypeIdentifier}


/**
  * This is a special Serializer that Serializes and deserializes nulls only.
  *
  * forked from akka.serialization.NullSerializer.
  */
class NullSerializer extends Serializer {
  val nullAsBytes = Array[Byte]()
  override def includeManifest: Boolean = false
  override def identifier: TypeIdentifier = TypeIdentifier.Undefined
  override def toBinary( o: AnyRef ): Array[Byte] = nullAsBytes
  @throws(classOf[NotSerializableException])
  override def fromBinary( bytes: Array[Byte], clazz: Option[Class[_]] ): AnyRef = null
}

object NullSerializer extends NullSerializer
