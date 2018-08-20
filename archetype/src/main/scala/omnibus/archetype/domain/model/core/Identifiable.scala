package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.identifier._

trait Identifiable {
  type E
  type TID = Id[E]
  def id: TID
}

object Identifiable {

  type Aux[E0, TID0] = Identifiable {
    type E = E0
    type TID = TID0
  }

  def apply( implicit i: Identifiable ): Aux[i.E, i.TID] = i

  def unapply( i: Identifiable ): Option[i.TID] = Option( i.id )
}

trait IdentifiableLensProvider[I <: Identifiable] {
  def idLens: Lens[I, I#TID]
}
