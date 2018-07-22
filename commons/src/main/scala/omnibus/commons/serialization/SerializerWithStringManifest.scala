package omnibus.commons.serialization

import java.io.NotSerializableException

/*
 * forked from akka.serialization.SerializerWithStringManifest.
 */
abstract class SerializerWithStringManifest extends Serializer {

  /**
    * Returns whether this serializer needs a manifest in the fromBinary method
    */
  final override def includeManifest: Boolean = true

  /**
    * Return the manifest (type hint) that will be provided in the fromBinary method.
    * Use `""` if manifest is not needed.
    */
  def manifest( o: AnyRef ): String

  /**
    * Produces an object from an array of bytes, with an optional type-hint;
    * the class should be loaded using ActorSystem.dynamicAccess.
    *
    * It's recommended to throw `java.io.NotSerializableException` in `fromBinary`
    * if the manifest is unknown. This makes it possible to introduce new class
    * types and send them to nodes that don't know about them. This may be helpful
    * when performing rolling upgrades, i.e. running a cluster with mixed
    * versions for while. The problem will be logged and data is dropped.
    */
  @throws( classOf[NotSerializableException] )
  def fromBinary( bytes: Array[Byte], manifest: String ): AnyRef

  /**
    * Produces an object from an array of bytes, with an optional type-hint;
    * the class should be loaded using ActorSystem.dynamicAccess.
    */
  override def fromBinary( bytes: Array[Byte], manifest: Option[Class[_]] ): AnyRef = {
    val manifestString = manifest map { _.getName } getOrElse { "" }
    fromBinary( bytes, manifestString )
  }
}
