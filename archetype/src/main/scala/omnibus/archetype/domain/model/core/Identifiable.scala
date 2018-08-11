package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.identifier._

trait Identifiable {
  type ID
  type TID = Id[ID]
  def id: TID
}

object Identifiable {

  type Aux[ID0, TID0] = Identifiable {
    type ID = ID0
    type TID = TID0
  }

  def apply( implicit i: Identifiable ): Aux[i.ID, i.TID] = i

  def unapply( i: Identifiable ): Option[i.TID] = Option( i.id )
}

trait IdentifiableLensProvider[I <: Identifiable] {
  def idLens: Lens[I, I#TID]
}
