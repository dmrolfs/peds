package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.core.syntax.clazz._

trait Entity extends Identifiable with Serializable {
  def name: String
  def slug: String = id.value.toString

  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}

object Entity {
  def unapply( e: Entity ): Option[( e.TID, String, String )] = Option( ( e.id, e.name, e.slug ) )
}

trait EntityLensProvider[E <: Entity] extends IdentifiableLensProvider[E] {
  def nameLens: Lens[E, String]
  def slugLens: Lens[E, String]
}
