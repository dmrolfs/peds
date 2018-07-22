package omnibus.commons.serialization

import java.io.NotSerializableException
import java.nio.ByteBuffer

import scala.util.control.NoStackTrace
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import omnibus.commons.serialization.serializers.{ ByteBufferSerializer, JavaSerializer }

/**
  * A Serializer represents a bimap between an object and an array of bytes representing that object.
  *
  * This mechanism is forked from akka.serialization.Serializer.
  */
trait Serializer {

  /**
    * Completely unique value to identify this implementation of Serializer, used to optimize network traffic.
    * Values from 0 to 40 are reserved for internal usage.
    */
  def identifier: TypeIdentifier

  /**
    * Serializes the given object into an Array of Byte
    */
  def toBinary( o: AnyRef ): Array[Byte]

  /**
    * Returns whether this serializer needs a manifest in the fromBinary method
    */
  def includeManifest: Boolean

  /**
    * Produces an object from an array of bytes, with an optional type-hint;
    * the class should be loaded using ActorSystem.dynamicAccess.
    */
  @throws( classOf[NotSerializableException] )
  def fromBinary( bytes: Array[Byte], manifest: Option[Class[_]] ): AnyRef

  /**
    * Java API: deserialize without type hint
    */
  final def fromBinary( bytes: Array[Byte] ): AnyRef = fromBinary( bytes, None )

  /**
    * Java API: deserialize with type hint
    */
  @throws( classOf[NotSerializableException] )
  final def fromBinary( bytes: Array[Byte], clazz: Class[_] ): AnyRef =
    fromBinary( bytes, Option( clazz ) )
}

object Serializer {

  /**
    * SECURITY BEST-PRACTICE is to disable java serialization for its multiple known attack surfaces.
    *
    * This setting is a short-cut to using DisabledJavaSerializer instead of JavaSerializer
    *
    * Completely disable the use of `com.here.platform.serialization.JavaSerializer` by the
    # serialization mechanism, instead DisabledJavaSerializer will
    # be inserted which will fail explicitly if attempts to use java serialization are made.
    #
    # The log messages emitted by such serializer SHOULD be treated as potential
    # attacks which the serializer prevented, as they MAY indicate an external operator
    # attempting to send malicious messages intending to use java serialization as attack vector.
    # The attempts are logged with the SECURITY marker.
    #
    # Please note that this option does not stop you from manually invoking java serialization
    #
    # The default value for this might be changed to off in future versions of Akka.

    */
  val AllowJavaSerializationKey = "here.olp.allow-java-serialization"

  /**
    * Entries for pluggable serializers and their bindings. For example,
    *    here.olp.serializers {
    *      java = com.here.platform.serialization.serializers.JavaSerializer
    *      bytes = com.here.platform.serialization.serializers.ByteBufferSerializer
    *    }
    */
  val SerializersKey = "here.olp.serializers"

  /**
    * Configuration namespace of serialization identifiers in the `reference.conf`.
    *
    * Each serializer implementation must have an entry in the following format:
    * `akka.actor.serialization-identifiers."FQCN" = ID`
    * where `FQCN` is fully qualified class name of the serializer implementation
    * and `ID` is globally unique serializer identifier number.
    */
  val SerializationIdentifiersKey = "here.olp.analytics.serialization-identifiers"
}

/**
  *  Base serializer trait with serialization identifiers configuration contract,
  *  when globally unique serialization identifier is configured in the `reference.conf`.
  *
  *  Forked from akka.serialization.BaseSerializer.
  */
trait BaseSerializer extends Serializer {
  def config: Config

  /**
    * Configuration namespace of serialization identifiers in the `reference.conf`.
    *
    * Each serializer implementation must have an entry in the following format:
    * `here.olp.analytics.serialization-identifiers."FQCN" = ID`
    * where `FQCN` is fully qualified class name of the serializer implementation
    * and `ID` is globally unique serializer identifier number.
    */
  final val SerializationIdentifiersKey = Serializer.SerializationIdentifiersKey

  /**
    * Globally unique serialization identifier configured in the `reference.conf`.
    *
    * See [[Serializer#identifier]].
    */
  override val identifier: TypeIdentifier =
    BaseSerializer.identifierFromConfig( this.getClass, config )
}

object BaseSerializer {

  /** INTERNAL API */
  private[platform] def identifierFromConfig( clazz: Class[_], config: Config ): TypeIdentifier = {
    config.as[TypeIdentifier]( s"""${Serializer.SerializationIdentifiersKey}."${clazz.getName}"""" )
  }
}

/**
  * Java API for creating a Serializer: make sure to include a constructor which
  * takes exactly one argument of type [[com.typesafe.config.Config]], because
  * that is the preferred constructor which will be invoked when reflectively instantiating
  * the JSerializer (also possible with empty constructor).
  *
  * forked from akka.serialization.JSerializer.
  */
abstract class JSerializer extends Serializer {

  @throws( classOf[NotSerializableException] )
  final override def fromBinary( bytes: Array[Byte], manifest: Option[Class[_]] ): AnyRef = {
    fromBinaryJava( bytes, manifest.orNull )
  }

  /**
    * This method must be implemented, manifest may be null.
    */
  protected def fromBinaryJava( bytes: Array[Byte], manifest: Class[_] ): AnyRef
}

/*
 * forked from akka.serialization.DisabledJavaSerializer.
 */
final case class DisabledJavaSerializer( config: Config )
    extends Serializer
    with ByteBufferSerializer {
  // use same identifier as JavaSerializer, since it's a replacement
  override val identifier: TypeIdentifier =
    BaseSerializer.identifierFromConfig( classOf[JavaSerializer], config )

  private[this] val empty = Array.empty[Byte]

//  private[this] val log = Logging.withMarker(system, getClass)

  def includeManifest: Boolean = false

  override def toBinary( o: AnyRef, buf: ByteBuffer ): Unit = {
//    log.warning(LogMarker.Security, "Outgoing message attempted to use Java Serialization even though `akka.actor.allow-java-serialization = off` was set! " +
//      "Message type was: [{}]", o.getClass)
    throw DisabledJavaSerializer.IllegalSerialization
  }

  @throws( classOf[NotSerializableException] )
  override def fromBinary( bytes: Array[Byte], clazz: Option[Class[_]] ): AnyRef = {
//    log.warning(LogMarker.Security, "Incoming message attempted to use Java Serialization even though `akka.actor.allow-java-serialization = off` was set!")
    throw DisabledJavaSerializer.IllegalDeserialization
  }

  @throws( classOf[NotSerializableException] )
  override def fromBinary( buf: ByteBuffer, manifest: String ): AnyRef = {
    // we don't capture the manifest or mention it in the log as the default setting for includeManifest is set to false.
//    log.warning(LogMarker.Security, "Incoming message attempted to use Java Serialization even though `akka.actor.allow-java-serialization = off` was set!")
    throw DisabledJavaSerializer.IllegalDeserialization
  }

  override def toBinary( o: AnyRef ): Array[Byte] = {
    toBinary( o, null )
    empty // won't return, toBinary throws
  }

}

object DisabledJavaSerializer {
  final class JavaSerializationException( msg: String )
      extends RuntimeException( msg )
      with NoStackTrace

  final val IllegalSerialization = new JavaSerializationException(
    s"""
      |Attempted to serialize message using Java serialization while `${Serializer.AllowJavaSerializationKey}` was
      |disabled. Check WARNING logs for more details.
    """.stripMargin
  )

  final val IllegalDeserialization = new JavaSerializationException(
    s"""
      |Attempted to deserialize message using Java serialization while `${Serializer.AllowJavaSerializationKey}` was
      |disabled. Check WARNING logs for more details.
    """.stripMargin
  )
}
