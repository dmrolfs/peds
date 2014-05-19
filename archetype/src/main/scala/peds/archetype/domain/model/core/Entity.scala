package peds.archetype.domain.model.core

import shapeless.Lens


trait Entity extends Identifiable {
  override type That <: Entity
  def name: String
  // def nameLens: Lens[this.type, String]
  def nameLens: Lens[That, String]

  override def toString: String = getClass.getSimpleName + s"(${name})"
}

object Entity {
  implicit def summarizeEntity( e: Entity ): EntitySummary = EntitySummary( e )
}
