package peds.archetype.domain.model.core

// import shapeless.Lens
import peds.commons.identifier._


trait Identifiable {
  type That <: Identifiable

  type ID
  def idClass: Class[_]

  type TID = Identifiable.TID[ID]
  // def idTag: Symbol
  // implicit def tag( id: ID ): TID = TaggedID( idTag, id )

  def id: TID
  // def idLens: Lens[this.type, ID]
  // def idLens: Lens[That, ID]
}

object Identifiable {
  type TID[I] = TaggedID[I]
}

trait IdentifiableCompanion[I <: Identifiable] {
  def nextId: Identifiable.TID[I#ID]
  def idTag: Symbol
  implicit def tag( id: I#ID ): Identifiable.TID[I#ID] = TaggedID( idTag, id )
}
