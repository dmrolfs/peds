package peds.archetype.domain.model.core

import shapeless.Lens
import peds.commons.identifier._


trait Identifiable {
  type ID
  def idClass: Class[_]

  type TID = Identifiable.TID[ID]

  def id: TID
}

object Identifiable {
  type TID[I] = TaggedID[I]
}

trait IdentifiableCompanion[I <: Identifiable] {
  def nextId: Identifiable.TID[I#ID]
  def idTag: Symbol
  def idLens: Lens[I, I#TID]
  implicit def tag( id: I#ID ): Identifiable.TID[I#ID] = TaggedID( idTag, id )
}
