package peds.commons.io


import java.io.{ ObjectOutputStream, ByteArrayOutputStream, ObjectInputStream, ByteArrayInputStream }


/**
 * A Serializer represents a bimap between an object and an array of bytes representing that object.
 *
 * Serializers are loaded using reflection during [[SUBSYSTEM]]
 * start-up, where two constructors are tried in order:
 *
 * <ul>
 * <li>taking exactly one argument of type [[akka.actor.ExtendedActorSystem]];
 * this should be the preferred one because all reflective loading of classes
 * during deserialization should use ExtendedActorSystem.dynamicAccess (see
 * [[akka.actor.DynamicAccess]]), and</li>
 * <li>without arguments, which is only an option if the serializer does not
 * load classes using reflection.</li>
 * </ul>
 *
 * <b>Be sure to always use the PropertyManager for loading classes!</b> This is necessary to
 * avoid strange match errors and inequalities which arise from different class loaders loading
 * the same class.
 */
// @deprecated( "want to replace with a more complete type class framework (eg, via play or spray) or even scala-pickling", "7/1/2013" )
trait Deserializer[Format, T] {

  /**
   * Produces an object from a Format type, with an optional type-hint;
   * the class should be loaded using ActorSystem.dynamicAccess.
   */
  def deserialize( src: Format )( implicit c: ConvertFrom[Format, T] ): Either[Throwable, T]
}
