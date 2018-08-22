package omnibus.archetype.domain.model.core

import shapeless.Lens
import omnibus.identifier._

trait IdentifiableLike {
  type E
  type TID = Id[E]
  def id: TID
}

object IdentifiableLike {

  type Aux[TID0] = IdentifiableLike { type TID = TID0 }

  def apply( implicit i: IdentifiableLike ): Aux[i.TID] = i

  //todo with or without Aux and type param?
  def unapply( i: IdentifiableLike ): Option[i.TID] = Option( i.id )
}

abstract class Identifiable[E0] extends IdentifiableLike {
  override type E = E0
}

object Identifiable {
  type Aux[E, TID0] = Identifiable[E] { type TID = TID0 }

  def apply[E]( implicit i: Identifiable[E] ): Aux[E, i.TID] = i

  //todo with or without Aux and type param?
  def unapply[E]( i: Identifiable[E] ): Option[i.TID] = Option( i.id )
}

abstract class IdentifiableLensProvider[E <: IdentifiableLike] {
  val identifying: Identifying[E]
  def idLens: Lens[E, identifying.TID]
}
