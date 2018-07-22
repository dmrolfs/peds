package omnibus.commons.serialization.serializers

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  NotSerializableException,
  ObjectOutputStream
}
import java.util.concurrent.Callable

import omnibus.commons.serialization.{
  BaseSerializer,
  ClassLoaderObjectInputStream,
  DisabledJavaSerializer
}
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

import scala.util.DynamicVariable

object JavaSerializer {

  /**
    * This holds a reference to the current configuration (the surrounding context)
    * during serialization and deserialization.
    *
    * If you are using Serializers yourself, outside of SerializationExtension,
    * you'll need to surround the serialization/deserialization with:
    *
    * currentConfiguration.withValue(config) {
    *   ...code...
    * }
    *
    * or
    *
    * currentConfiguration.withValue(config, callable)
    */
  val currentConfiguration: CurrentConfiguration = new CurrentConfiguration
  final class CurrentConfiguration extends DynamicVariable[Config]( null ) {

    /**
      * Java API: invoke the callable with the current system being set to the given value for this thread.
      *
      * @param value - the current value under the call to callable.call()
      * @param callable - the operation to be performed
      * @return the result of callable.call()
      */
    def withValue[S]( value: Config, callable: Callable[S] ): S =
      super.withValue[S]( value )( callable.call )
  }
}

/**
  * This Serializer uses standard Java Serialization
  *
  * forked from akka.serialization.JavaSerializer.
  */
class JavaSerializer( override val config: Config ) extends BaseSerializer {
  val allowJavaSerialization = config.as[Boolean]( JavaSerializer.AllowJavaSerializationKey )
  if (!allowJavaSerialization) {
    throw new DisabledJavaSerializer.JavaSerializationException(
      s"Attempted creation of `JavaSerializer` while `${JavaSerializer.AllowJavaSerializationKey} = off` was set!"
    )
  }

  override def includeManifest: Boolean = false

  override def toBinary( o: AnyRef ): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val out = new ObjectOutputStream( bos )
    JavaSerializer.currentConfiguration.withValue( config ) { out.writeObject( o ) }
    out.close()
    bos.toByteArray
  }

  @throws( classOf[NotSerializableException] )
  def fromBinary( bytes: Array[Byte], clazz: Option[Class[_]] ): AnyRef = {
    val in = new ClassLoaderObjectInputStream(
      this.getClass.getClassLoader,
      new ByteArrayInputStream( bytes )
    )
    val obj = JavaSerializer.currentConfiguration.withValue( config ) { in.readObject }
    in.close()
    obj
  }
}
