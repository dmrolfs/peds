package peds.archetype.domain.model.core

import scala.language.existentials
import peds.commons.util._


case class EntityRef( 
  id: Entity#ID, 
  clazz: Class[_ <: Entity], 
  meta: Map[Symbol, Any] = Map.empty 
) extends Equals with Ordered[EntityRef] {
  def this( id: Entity#ID, clazz: Class[_ <: Entity], metaProperties: (Symbol, Any)* ) = {
    this( id, clazz, Map( metaProperties:_* ) )
  }

  def this( entity: Entity, metaProperties: (Symbol, Any)* ) = {
    this( entity.id, entity.getClass, Map( ( metaProperties :+ ( EntityRef.name -> entity.name ) ):_* ) )
  }

  override def compare( rhs: EntityRef ): Int = this.## compare rhs.##

  override def hashCode: Int = {
    41 * (
      41 * (
        41 + id.##
      ) + clazz.##
    )
  }

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[EntityRef]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: EntityRef => {
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
    import EntityRef._
    var result = "EntityRef("
    result += meta.get(name) map { name => s"name=$name" } getOrElse s"id=$id"
    result += s", class=${clazz.safeSimpleName}"
    if ( !meta.isEmpty ) result += s", meta=${meta}"
    result += ")"
    result
  }
}

object EntityRef {
  val name: Symbol = 'name

  def apply( id: Entity#ID, clazz: Class[_ <: Entity], metaProperties: (Symbol, Any)* ): EntityRef = {
    new EntityRef( id, clazz, metaProperties:_* )
  }
  
  def apply( entity: Entity, metaProperties: (Symbol, Any)* ): EntityRef = {
    new EntityRef( entity, metaProperties:_* )
  }
}
