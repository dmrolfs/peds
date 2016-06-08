package peds.archetype.domain.model.core

import shapeless.Lens
import peds.commons.identifier._

import scala.reflect.ClassTag


trait Identifiable {
  type ID
  def evId: ClassTag[ID]

  type TID = TaggedID[ID]

  def id: TID
}


trait IdentifiableCompanion[I <: Identifiable] {
  def nextId: I#TID
  def idTag: Symbol
  implicit def tag( id: I#ID ): I#TID
}

trait IdentifiableLensedCompanion[I <: Identifiable] extends IdentifiableCompanion[I] {
  def idLens: Lens[I, I#TID]
}
