package omnibus.commons.serialization.serializers

import java.io.NotSerializableException
import java.nio.ByteBuffer

/**
  * Serializer between an object and a `ByteBuffer` representing that object.
  *
  * Implementations should typically extend [[omnibus.commons.serialization.SerializerWithStringManifest]] and
  * in addition to the `ByteBuffer` based `toBinary` and `fromBinary` methods also
  * implement the array based `toBinary` and `fromBinary` methods. The array based
  * methods will be used when `ByteBuffer` is not used, e.g. in Akka Persistence.
  *
  * Note that the array based methods can for example be implemented by delegation
  * like this:
  * {{{
  *   // you need to know the maximum size in bytes of the serialized messages
  *   val pool = new akka.io.DirectByteBufferPool(defaultBufferSize = 1024 * 1024, maxPoolEntries = 10)
  *
  *
  *  // Implement this method for compatibility with `SerializerWithStringManifest`.
  *  override def toBinary(o: AnyRef): Array[Byte] = {
  *    val buf = pool.acquire()
  *    try {
  *      toBinary(o, buf)
  *      buf.flip()
  *      val bytes = new Array[Byte](buf.remaining)
  *      buf.get(bytes)
  *      bytes
  *    } finally {
  *      pool.release(buf)
  *    }
  *  }
  *
  *  // Implement this method for compatibility with `SerializerWithStringManifest`.
  *  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
  *    fromBinary(ByteBuffer.wrap(bytes), manifest)
  *
  * }}}
  *
  * Forked from akka.serialization.ByteBufferSerializer
  */
trait ByteBufferSerializer {

  /**
    * Serializes the given object into the `ByteBuffer`.
    */
  def toBinary( o: AnyRef, buf: ByteBuffer ): Unit

  /**
    * Produces an object from a `ByteBuffer`, with an optional type-hint;
    * the class should be loaded using ActorSystem.dynamicAccess.
    */
  @throws( classOf[NotSerializableException] )
  def fromBinary( buf: ByteBuffer, manifest: String ): AnyRef
}
