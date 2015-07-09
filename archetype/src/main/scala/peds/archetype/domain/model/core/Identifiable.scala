package peds.archetype.domain.model.core

// import shapeless.Lens
import peds.commons.identifier._


trait Identifiable {
  type That <: Identifiable

  type ID
  def idClass: Class[_]

  type TID = TaggedID[ID]
  def idTag: Symbol
  implicit def tag( id: ID ): TID = TaggedID( idTag, id )

  def id: TID
  // def idLens: Lens[this.type, ID]
  // def idLens: Lens[That, ID]
}
