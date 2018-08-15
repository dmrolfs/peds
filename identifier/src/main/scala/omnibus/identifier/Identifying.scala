package omnibus.identifier

import scala.reflect._
import omnibus.core._

abstract class Identifying[E: ClassTag] {
  type ID

  implicit def label: Label = new MakeLabel[E]

  implicit protected val self: Identifying.Aux[E, ID] = this.asInstanceOf[Identifying.Aux[E, ID]]

  final def zero: Id[E] = Id.of( zeroValue )
  final def next: Id[E] = Id.of( nextValue )
  final def fromString( rep: String ): Id[E] = Id.of( valueFromRep( rep ) )

  def zeroValue: ID
  def nextValue: ID
  def valueFromRep( rep: String ): ID
  override def toString: String = {
    val entity = classTag[E].runtimeClass.safeSimpleName
    val idType = zeroValue.getClass.safeSimpleName
    s"""Identifying[${entity}](label:"${label.label}" id:${idType})"""
  }
}

object Identifying {
  type Aux[E, I] = Identifying[E] { type ID = I }
}
