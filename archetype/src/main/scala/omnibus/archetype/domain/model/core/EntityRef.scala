package omnibus.archetype.domain.model.core

import scala.collection.immutable
import scala.reflect.ClassTag
import enumeratum._
import omnibus.core.syntax.clazz._
import omnibus.identifier.Identifying

sealed trait EntityRef extends (( Symbol ) => Any ) with Ordered[EntityRef] {
  type Source
  type TID
//  type ID = Source#ID
//  type TID = Source#TID

  def get( property: Symbol ): Option[Any]

  def id: TID
  def name: String
  def entityType: ClassTag[_]

  def identify: String = name + ":" + id
  override def toString: String = identify
}

object EntityRef {

  type Aux[E, TID0] = EntityRef {
    type Source = E
    type TID = TID0
  }

  def apply( implicit ref: EntityRef ): Aux[ref.Source, ref.TID] = ref

  sealed abstract class StandardProperty extends EnumEntry {
    def unapply( key: Symbol ): Boolean = this.key == key
    def key: Symbol = Symbol( entryName.toLowerCase )
  }

  object StandardProperty extends Enum[StandardProperty] {
    override def values: immutable.IndexedSeq[StandardProperty] = findValues

    case object Id extends StandardProperty
    case object Name extends StandardProperty
  }

  def apply[ID, E <: Entity[E, ID]](
    entity: E
  )(
    implicit identifying: Identifying.Aux[E, ID],
    entityType: ClassTag[E]
  ): EntityRef.Aux[E, entity.TID] = {
    val eid = entity.id

    SimpleEntityRef[entity.TID, ID, E](
      id = eid, //.asInstanceOf[TID], //todo: it'll match and I shouldn't have to cast
      name = entity.name,
      meta = Map.empty[Symbol, Any]
    )
  }

  // def apply( e: Entity, meta: (Symbol, Any)* ): EntityRef = {
  //   EntityRefImpl( id = e.id, name = e.name, clazz = e.getClass, meta = Map( meta:_* ) )
  // }

  // def apply[T <: Entity : ClassTag]( id: T#ID, name: String ): EntityRef = {
  //   EntityRefImpl[T]( id = id, name = name, clazz = implicitly[ClassTag[T]].runtimeClass )
  // }

  // def apply( id: Entity#ID, name: String, clazz: Class[_], meta: (Symbol, Any)* ): EntityRef = {
  //   EntityRefImpl( id = id, name = name, clazz = clazz, meta = Map( meta:_* ) )
  // }

  def unapply( ref: EntityRef ): Option[( ref.TID, String )] = Some( ( ref.id, ref.name ) )

  case class SimpleEntityRef[TID0, ID, E <: Entity[E, ID]: ClassTag] private (
    override val id: TID0,
    override val name: String,
    override val meta: Map[Symbol, Any]
  ) extends EntityRefLike {
    override type Source = E
    override type TID = TID0
    override val entityType: ClassTag[_] = implicitly[ClassTag[E]]
  }

  trait EntityRefLike extends EntityRef with Equals {
    def meta: Map[Symbol, Any]

    override def apply( property: Symbol ): Any = {
      get( property ) getOrElse {
        throw new NoSuchElementException(
          s"${toString} does not contain requested property: $property"
        )
      }
    }

    override def get( property: Symbol ): Option[Any] = property match {
      case StandardProperty.Id()   => Some( id )
      case StandardProperty.Name() => Some( name )
      case p                       => meta get p
    }

    override def compare( rhs: EntityRef ): Int = this.## compare rhs.##

    override def hashCode: Int = 41 * (41 + id.##)

    override def equals( rhs: Any ): Boolean = rhs match {
      case that: EntityRefLike => {
        if (this eq that) true
        else {
          (that.## == this.##) &&
          (that canEqual this) &&
          (this.id == that.id)
        }
      }

      case _ => false
    }

    override def toString: String = {
      var result = s"EntityRef(name=$name, id=$id, class=${getClass.safeSimpleName}"
      if (!meta.isEmpty) result += s", meta=${meta}"
      result += ")"
      result
    }
  }
}
