package peds.archetype.domain.model.core

import shapeless.Lens
import peds.commons.identifier._


trait Identifiable {
  type ID
  def idClass: Class[_]

  type TID = TaggedID[ID]

  def id: TID
}

// object Identifiable {
//   type TID[I] = TaggedID[I]
// }

trait IdentifiableCompanion[I <: Identifiable] {
  // type ID = I#ID
  // type TID = I#TID
  def nextId: I#TID
  def idTag: Symbol
  implicit def tag( id: I#ID ): I#TID
}

trait IdentifiableLensedCompanion[I <: Identifiable] extends IdentifiableCompanion[I] {
  def idLens: Lens[I, I#TID]
}
