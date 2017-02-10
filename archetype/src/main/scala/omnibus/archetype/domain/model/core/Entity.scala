package omnibus.archetype.domain.model.core

import scala.reflect.ClassTag
import shapeless.Lens
import omnibus.commons.identifier.Identifying
import omnibus.commons.util._


trait Entity extends Identifiable with Serializable {
  def name: String
  def slug: String = id.get.toString

  override def toString: String = s"${getClass.safeSimpleName}(${name})"
}


trait EntityIdentifying[E <: Entity] extends Identifying[E] {
  val evEntity: ClassTag[E]
  override type ID = E#ID
  override def idOf( o: E ): TID = tag( o.id )

  private val Entity = """(\w+)Entity""".r
  override lazy val idTag: Symbol = {
    val tag = evEntity.runtimeClass.safeSimpleName match {
      case Entity(t) => t
      case t => t
    }

    Symbol( tag )
  }
}


trait EntityLensProvider[E <: Entity] extends IdentifiableLensProvider[E] {
  def nameLens: Lens[E, String]
  def slugLens: Lens[E, String]
}
