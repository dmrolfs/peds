package omnibus.commons.serialization

import java.io.NotSerializableException
import java.util.concurrent.atomic.AtomicReference

import scala.util.{ Failure, Success, Try }
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

import scala.annotation.tailrec

class Serialization( config: Config ) {
  val allowJavaSerialization: Boolean = config.as[Boolean]( Serializer.AllowJavaSerializationKey )

  type ManifestCache = AtomicReference[Map[String, Option[Class[_]]]]
  private val manifestCache: ManifestCache = new AtomicReference[Map[String, Option[Class[_]]]](
    Map.empty[String, Option[Class[_]]]
  )

  /**
    * Serializes the given AnyRef/java.lang.Object according to the Serialization configuration
    * to either an Array of Bytes or an Exception if one was thrown.
    */
  def serialize( o: AnyRef ): Try[Array[Byte]] = Try { findSerializerFor( o ) toBinary o }

  /**
    * Deserializes the given array of bytes using the specified serializer id,
    * using the optional type hint to the Serializer.
    * Returns either the resulting object or an Exception if one was thrown.
    */
  def deserialize[T](
    bytes: Array[Byte],
    serializerId: TypeIdentifier,
    clazz: Option[Class[_ <: T]]
  ): Try[T] = {
    Try {
      val serializer = try {
        getSerializerById( serializerId )
      } catch {
        case _: NoSuchElementException =>
          throw new NotSerializableException(
            s"Cannot find serializer with id [$serializerId]. The most probable reason is that the configuration entry " +
            s"${Serializer.SerializersKey} is not in synch between the two systems."
          )
      }

      serializer.fromBinary( bytes, clazz ).asInstanceOf[T]
    }
  }

  /**
    * Deserializes the given array of bytes using the specified serializer id,
    * using the optional type hint to the Serializer.
    * Returns either the resulting object or an Exception if one was thrown.
    */
  def deserialize(
    bytes: Array[Byte],
    serializerId: TypeIdentifier,
    manifest: String
  ): Try[AnyRef] = {
    Try {
      val serializer = try {
        getSerializerById( serializerId )
      } catch {
        case _: NoSuchElementException ⇒
          throw new NotSerializableException(
            s"Cannot find serializer with id [$serializerId]. The most probable reason is that the configuration entry " +
            s"${Serializer.SerializersKey} is not in synch between the two systems."
          )
      }

      deserializeByteArray( bytes, serializer, manifest )
    }
  }

  private def deserializeByteArray(
    bytes: Array[Byte],
    serializer: Serializer,
    manifest: String
  ): AnyRef = {

    @tailrec def updateCache(
      cache: Map[String, Option[Class[_]]],
      key: String,
      value: Option[Class[_]]
    ): Boolean = {
      manifestCache.compareAndSet( cache, cache.updated( key, value ) ) ||
      updateCache( manifestCache.get, key, value ) // recursive, try again
    }

    serializer match {
      case s2: SerializerWithStringManifest => s2.fromBinary( bytes, manifest )
      case s1 => {
        if (manifest == "") s1.fromBinary( bytes, None )
        else {
          val cache = manifestCache.get
          cache.get( manifest ) match {
            case Some( cachedClassManifest ) => s1.fromBinary( bytes, cachedClassManifest )
            case None => {
              system.dynamicAccess.getClassFor[AnyRef]( manifest ) match {
                case Success( classManifest ) => {
                  val classManifestOption: Option[Class[_]] = Some( classManifest )
                  updateCache( cache, manifest, classManifestOption )
                  s1.fromBinary( bytes, classManifestOption )
                }

                case Failure( e ) => {
                  throw new NotSerializableException(
                    s"Cannot find manifest class [$manifest] for serializer with id [${serializer.identifier}]."
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  WORK HERE
}
