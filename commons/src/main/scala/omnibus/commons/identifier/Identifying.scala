package omnibus.commons.identifier

import scala.reflect.ClassTag
import omnibus.commons.ErrorOr
import omnibus.commons.util._


/** Type class used to define fundamental ID operations for a specific type of identifier.
  *
  * @tparam T - type for which identifiers are defined
  */
abstract class Identifying[T] {
  type ID
  type TID = TaggedID[ID]
  def idTag: Symbol
  implicit def tag( id: ID ): TID = TaggedID[ID]( idTag, id )

  def tidOf( o: T ): TID
  def nextTID: ErrorOr[TID]
  def idFromString( idRep: String ): ID
  def tidFromString( idRep: String ): TID = idFromString( idRep )

  override def toString: String = s"Identifying(${idTag})"
}

object Identifying {
  type Aux[T, ID0] = Identifying[T] { type ID = ID0 }

  /** summoner operation for Identifying instances
    */
  def apply[T]( implicit i: Identifying[T] ): Aux[T, i.ID] = i

  def optionIdentifying[T, I0]( implicit tid: Identifying.Aux[T, I0] ): Identifying.Aux[Option[T], I0] = {
    new Identifying[Option[T]] {
      override type ID = tid.ID
      override val idTag: Symbol = tid.idTag
      override def tidOf( obj: Option[T] ): TID = tag( tid.tidOf( obj.get ).id )
      override def nextTID: ErrorOr[TID] = tid.nextTID map { id => tag( id.id ) }
      override def idFromString( idRep: String ): ID = tid idFromString idRep
    }
  }

  final case class NotDefinedForId[T: ClassTag]( operation: String )
    extends IllegalStateException(
      s"${operation} is not defined for type: ${shapeless.the[ClassTag[T]].runtimeClass.safeSimpleName}"
    )
}
