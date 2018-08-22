package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.core.syntax.clazz._

trait EntityLike extends IdentifiableLike with Serializable {
  def name: String
  def slug: String = id.value.toString

  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}

object EntityLike {

  def unapply( e: EntityLike ): Option[( e.TID, String, String )] =
    Option( ( e.id, e.name, e.slug ) )
}

abstract class Entity[E] extends Identifiable[E] with EntityLike

object Entity {
  type Aux[E, TID0] = Entity[E] { type TID = TID0 }

  def unapply[E]( e: Entity[E] ): Option[( e.TID, String, String )] = {
    Option( ( e.id, e.name, e.slug ) )
  }
}

trait EntityLensProvider[E <: EntityLike] extends IdentifiableLensProvider[E] {
  def nameLens: Lens[E, String]
  def slugLens: Lens[E, String]
}
