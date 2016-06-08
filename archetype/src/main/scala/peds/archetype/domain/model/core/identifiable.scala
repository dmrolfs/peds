package peds.archetype.domain.model.core

import scala.reflect.ClassTag
import shapeless.Lens
import peds.commons.TryV
import peds.commons.identifier._



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


/**
  * Type class used to define fundamental ID operations for a specific type of identifier.
  * @tparam I
  */
trait Identifying[I] {
  def nextId: TryV[I]
}

object Identifying {
  implicit val idShortUUID: Identifying[ShortUUID] = new Identifying[ShortUUID] {
    import scalaz.syntax.either._
    override def nextId: TryV[ShortUUID] = ShortUUID().right
  }
}