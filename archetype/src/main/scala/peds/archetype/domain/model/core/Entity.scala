package peds.archetype.domain.model.core

import shapeless.Lens
import peds.commons.util._


trait Entity extends Identifiable {
  def name: String
  def slug: String = id.get.toString

  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}


trait EntityIdentifying[E <: Entity] extends Identifying[E] {
  override type ID = E#ID
  override def idOf( o: E ): TID = o.id
}


trait EntityLensProvider[E <: Entity] extends IdentifiableLensProvider[E] {
  def nameLens: Lens[E, String]
  def slugLens: Lens[E, String]
}
