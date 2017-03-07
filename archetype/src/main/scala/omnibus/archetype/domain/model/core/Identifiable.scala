package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.commons.identifier._


trait Identifiable {
  type ID
  type TID = TaggedID[ID]
  def id: TID
}

object Identifiable {
  type Aux[ID0] = Identifiable { type ID = ID0 }

  def apply( implicit i: Identifiable ): Aux[i.ID] = i
}

trait IdentifiableLensProvider[I <: Identifiable] {
  def idLens: Lens[I, I#TID]
}
