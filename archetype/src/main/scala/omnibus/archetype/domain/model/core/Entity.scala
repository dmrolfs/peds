package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.core.syntax.clazz._
import omnibus.identifier.Identifying

abstract class Entity[E <: Entity[E, ID], ID](
  implicit override protected val identifying: Identifying.Aux[E, ID]
) extends Identifiable[E, ID] {
  def name: String
  def slug: String = id.value.toString
  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}

object Entity {

  def unapply[E <: Entity[E, ID], ID]( e: Entity[E, ID] ): Option[( e.TID, String, String )] = {
    Some( ( e.id, e.name, e.slug ) )
  }
}
//trait EntityLike extends IdentifiableLike with Serializable {
//  def name: String
//  def slug: String = id.value.toString
//
//  override def toString: String = s"${getClass.safeSimpleName}(${name})"
//}
//
//object EntityLike {
//
//  type Aux[E0, TID0] = EntityLike {
//    type E = E0
//    type TID = TID0
//  }
//
//  def unapply( e: EntityLike ): Option[( e.TID, String, String )] =
//    Option( ( e.id, e.name, e.slug ) )
//}
//
//abstract class Entity[E <: EntityLike, ID](
//  override implicit val identifying: Identifying.IdAux[E, ID]
//) extends Identifiable[E, ID]
//    with EntityLike
//
//object Entity {
//
//  def unapply[E <: EntityLike, ID]( e: Entity[E, ID] ): Option[( e.TID, String, String )] = {
//    Option( ( e.id, e.name, e.slug ) )
//  }
//}

trait EntityLensProvider[E <: Entity[E, ID], ID] extends IdentifiableLensProvider[E, ID] {
  def nameLens: Lens[E, String]
  def slugLens: Lens[E, String]
}
