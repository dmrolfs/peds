package peds.archetype.domain.model.core

import scala.reflect.ClassTag
import scala.language.existentials
import peds.commons.util._


sealed trait EntityRef extends ((Symbol) => Any) with Ordered[EntityRef] {
  type Source <: Entity
  type ID = Source#ID

  def get( property: Symbol ): Option[Any]

  def id: ID
  def name: String
  def clazz: Class[_]
}

object EntityRef {
  val ID: Symbol = 'id
  val NAME: Symbol = 'name
  val CLAZZ: Symbol = 'class


  // def apply[T <: Entity : ClassTag]( entity: T ): EntityRef = {
  //   EntityRefImpl[T]( id = entity.id, name = entity.name, clazz = implicitly[ClassTag[T]].runtimeClass )
  // }

  def apply[T <: Entity : ClassTag]( entity: T, meta: (Symbol, Any)* ): EntityRef = {
    EntityRefImpl[T](
      id = entity.id,
      name = entity.name,
      clazz = entity.getClass,
      meta = Map( meta:_* )
    )
  }

  // def apply[T <: Entity : ClassTag]( id: T#ID, name: String ): EntityRef = {
  //   EntityRefImpl[T]( id = id, name = name, clazz = implicitly[ClassTag[T]].runtimeClass )
  // }

  def apply[T <: Entity : ClassTag]( id: T#ID, name: String, clazz: Class[_], meta: (Symbol, Any)* ): EntityRef = {
    EntityRefImpl[T](
      id = id,
      name = name,
      clazz = clazz,
      meta = Map( meta:_* )
    )
  }

  def unapply( ref: EntityRef ): Option[(ref.ID, String, Class[_])] = Some( (ref.id, ref.name, ref.clazz) )

  private case class EntityRefImpl[T <: Entity : ClassTag]( 
    override val id: T#ID, 
    override val name: String, 
    override val clazz: Class[_],
    meta: Map[Symbol, Any] = Map()
  ) extends EntityRef with Equals {
    override type Source = T

    override def apply( property: Symbol ): Any = {
      get( property ) getOrElse {
        throw new NoSuchElementException( s"${toString} does not contain requested property: $property" )
      }
    }

    override def get( property: Symbol ): Option[Any] = property match {
      case ID => Some( id )
      case NAME => Some( name )
      case CLAZZ => Some( clazz )
      case p => meta get p
    }

    override def compare( rhs: EntityRef ): Int = this.## compare rhs.##

    override def hashCode: Int = {
      41 * (
        41 * (
          41 + id.##
        ) + clazz.##
      )
    }

    override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[EntityRefImpl[T]]

    override def equals( rhs: Any ): Boolean = rhs match {
      case that: EntityRefImpl[T] => {
        if ( this eq that ) true
        else {
          ( that.## == this.## ) &&
          ( that canEqual this ) &&
          ( this.id == that.id ) &&
          ( this.clazz == that.clazz )
        }
      }

      case _ => false
    }

    override def toString: String = {
      var result = s"EntityRef(name=$name, id=$id, class=${clazz.safeSimpleName}"
      if ( !meta.isEmpty ) result += s", meta=${meta}"
      result += ")"
      result
    }
  }
}
