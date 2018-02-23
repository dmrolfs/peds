package omnibus.commons.io


/**
 * A Serializer represents a bimap between an object and an array of bytes representing that object.
 *
 * Serializers are loaded using reflection during SUBSYSTEM
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
@deprecated( "use serialization package", "0.63" )
trait Serializer[T, Format] {

  /**
   * Serializes the given object into a Format type
   */
  def serialize( o: T )( implicit c: ConvertFrom[T, Format] ): Either[Throwable, Format]
}
