package peds.commons.partial

import scala.annotation.tailrec
import scala.xml.{Elem, Node}
import peds.commons.math.OrderingHelper


trait XmlReducable {
  import XmlReducable._

  implicit def xmlReducable( implicit xform: Transformable[Elem] ): Reducable[Elem] = new Reducable[Elem] {
    override def elide( data: Elem, spec: ElisionCriteria ): Elem = trace.block( "elide" ) {
      @tailrec def loop( nodes: List[Node], spec: ElisionCriteria, elisions: Vector[Node] ): Vector[Node] = /*trace.block("loop")*/ {
        trace( "spec="+spec )
        trace( "nodes="+nodes.map(_.label).mkString("[",",","]") )

        nodes match {
          case (head: Elem) :: tail if spec.contains( head.label ) && spec.get( head.label ).get.isComposite => {
            trace( "head="+head.label )
            trace( "matched Elem with spec and composite")
            loop( 
              tail, 
              spec, 
              elisions :+ elide( head, spec.get( head.label ).get )
            )
          }
          case head :: tail if spec.contains( head.label ) => {
            trace( "head="+head.label )
            trace( "matched Elem with spec and not composite")
            loop( tail, spec, elisions :+ head )
          }
          case head :: tail => {
            trace( "head="+head.label )
            trace( "matched Elem but not in spec")
            loop( tail, spec, elisions )
          }
          case Nil => {
            trace( "matched Nil")
            elisions
          }
        }
      }

      trace( "spec="+spec )
      val elisions = loop( 
        xform.transform( data, spec ).child.toList, 
        spec, 
        Vector.empty 
      )
      trace( "elisions="+elisions )
      Elem( 
        prefix = data.prefix, 
        label = data.label, 
        attributes = data.attributes, 
        scope = data.scope, 
        minimizeEmpty = data.minimizeEmpty,
        child = elisions:_* 
      )
    }
  }
}

object XmlReducable {
  lazy val trace = peds.commons.log.Trace[XmlReducable]

  case object XmlTransformable extends Transformable[Elem] {
    import Transformable._

    override def execute = sort

    val ordering: Ordering[Node] = Ordering.by[Node, String]{ _.text }

    def sort: PartialFunction[(Elem, ElemPropValue), Elem] = {
      case (data, epv) if epv._2 == "sort-asc" => sortByEpv( data, epv )( ordering )
      case (data, epv) if epv._2 == "sort-desc" => sortByEpv( data, epv )( ordering.reverse )
    }

    private def sortByEpv( data: Elem, epv: ElemPropValue )( implicit ord: Ordering[Node] ): Elem = {
      implicit val OptChildOrdering: Ordering[Option[Node]] = OrderingHelper.makeOptionOrdering[Node]
      val (target, excl) = data.child partition { n => n.label == epv._1 }
      val children = excl ++ ( target sortBy { c => ( c \ epv._3 ).headOption } )
      Elem( 
        prefix = data.prefix, 
        label = data.label, 
        attributes = data.attributes, 
        scope = data.scope,
        minimizeEmpty = data.minimizeEmpty,
        child = children:_* 
      )
    }
  }
}