package peds.archetype.domain.model.core

// import scala.concurrent.Future
// import org.joda.{time => joda}
import rillit._
// import peds.commons.Clock
import scala.language.existentials


case class EntitySummary( 
  id: Entity#ID, 
  clazz: Class[_], 
  meta: Map[Symbol, Any] = Map.empty 
) extends Equals with Ordered[EntitySummary] {
  def this( id: Entity#ID, clazz: Class[_], metaProperties: (Symbol, Any)* ) = {
    this( id, clazz, Map( metaProperties:_* ) )
  }

  def this( entity: Entity, metaProperties: (Symbol, Any)* ) = {
    this( entity.id.get, entity.getClass, Map( ( metaProperties :+ ( EntitySummary.name -> entity.name ) ):_* ) )
  }

  override def compare( rhs: EntitySummary ): Int = this.## compare rhs.##

  override def hashCode: Int = {
    41 * (
      41 * (
        41 + id.##
      ) + clazz.##
    )
  }

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[EntitySummary]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: EntitySummary => {
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
    import EntitySummary._
    var result = "EntitySummary("
    result += meta.get(name) map { name => s"name=$name" } getOrElse s"id=$id"
    result += s", class=${clazz.getSimpleName}"
    if ( !meta.isEmpty ) result += s", meta=${meta}"
    result += ")"
    result
  }
}

object EntitySummary {
  val name: Symbol = 'name

  def apply( id: Entity#ID, clazz: Class[_], metaProperties: (Symbol, Any)* ): EntitySummary = {
    new EntitySummary( id, clazz, metaProperties:_* )
  }
  
  def apply( entity: Entity, metaProperties: (Symbol, Any)* ): EntitySummary = {
    new EntitySummary( entity, metaProperties:_* )
  }
}
