package omnibus.commons.partial

import scala.annotation.tailrec
import scala.xml.{Elem, Node}
import journal._
import omnibus.commons.math.OrderingHelper


trait XmlReducable {
  private val log = Logger[XmlReducable]

  implicit def xmlReducable( implicit xform: Transformable[Elem] ): Reducable[Elem] = new Reducable[Elem] {
    override def elide( data: Elem, spec: PartialCriteria ): Elem = {
      @tailrec def loop( nodes: List[Node], spec: PartialCriteria, partials: Vector[Node] ): Vector[Node] = /*trace.block("loop")*/ {
        log.debug( "spec="+spec )
        log.debug( "nodes="+nodes.map(_.label).mkString("[",",","]") )

        nodes match {
          case (head: Elem) :: tail if spec.contains( head.label ) && spec.get( head.label ).get.isComposite => {
            log.debug( "head="+head.label )
            log.debug( "matched Elem with spec and composite")
            loop( 
              tail, 
              spec, 
              partials :+ elide( head, spec.get( head.label ).get )
            )
          }
          case head :: tail if spec.contains( head.label ) => {
            log.debug( "head="+head.label )
            log.debug( "matched Elem with spec and not composite")
            loop( tail, spec, partials :+ head )
          }
          case head :: tail => {
            log.debug( "head="+head.label )
            log.debug( "matched Elem but not in spec")
            loop( tail, spec, partials )
          }
          case Nil => {
            log.debug( "matched Nil")
            partials
          }
        }
      }

      log.debug( "spec="+spec )
      val partials = loop( 
        xform.transform( data, spec ).child.toList, 
        spec, 
        Vector.empty 
      )
      log.debug( "partials="+partials )
      Elem( 
        prefix = data.prefix, 
        label = data.label, 
        attributes = data.attributes, 
        scope = data.scope, 
        minimizeEmpty = data.minimizeEmpty,
        child = partials:_* 
      )
    }
  }
}

object XmlReducable {
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