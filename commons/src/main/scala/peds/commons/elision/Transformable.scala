package peds.commons.elision

import scala.annotation.implicitNotFound


@implicitNotFound( msg = "Cannot find Transformable type class for ${A}" )
trait Transformable[A] {
  import Transformable._

  def execute: PartialFunction[(A, ElemPropValue), A]

  def transform( data: A, spec: ElisionCriteria ): A = /*trace.block( "transform" )*/ {
    epvsFor( spec ).foldLeft( data ){ (d, epv) => if ( execute.isDefinedAt( (d, epv) ) ) execute( (d, epv) ) else d }
  }

  private def epvsFor( spec: ElisionCriteria ): List[ElemPropValue] = {
    val parent = Nil.zipAll( spec.properties.toList, "", null )
    val children = spec.withProperties.map{ kv => 
      Nil.zipAll( kv._2.properties.toList, kv._1, null ) 
    }.toList.flatten
    ( parent ++ children ).map{ t => ( t._1, t._2._1, t._2._2 ) }
  }
}

object Transformable {
  type ElemPropValue = Tuple3[String, String, String]
  trait Transform[A] extends Function2[A, ElemPropValue, A]

  lazy val trace = peds.commons.log.Trace( "Transformable" )
}


case class NilTransformable[A]() extends Transformable[A] {
  import Transformable._
  override def execute: PartialFunction[(A, ElemPropValue), A] = { case (data, _) => data }
  override def transform( data: A, spec: ElisionCriteria ): A = data
}