package peds.commons.elision

import scala.annotation.tailrec
import spray.json._


trait JsonReducable {
  import JsonReducable._

  implicit def jsonReducable( implicit xform: Transformable[JsValue] ): Reducable[JsValue] = new Reducable[JsValue] {
    override def elide( data: JsValue, spec: ElisionCriteria ): JsValue = {
      data match {
        case o: JsObject => /*trace.block( "elide-OBJECT("+spec+")" )*/ { 
          @tailrec def loop( nodes: List[JsField], spec: ElisionCriteria, elisions: Vector[JsField] ): Vector[JsField] = {
            // trace( "loop-head="+nodes.headOption.getOrElse("{}") )
            // trace( "loop-spec="+spec )
            // trace( "loop-elisions="+elisions )
            nodes match {
              case head :: tail if spec.contains( head._1 ) => { 
                // trace( "head MATCH spec. head="+head )
                val nextSpec = spec.get( head._1 ).getOrElse( spec )
                loop( tail, spec, elisions :+ ( head._1 -> elide( head._2, nextSpec ) ) )
              }

              case head :: tail => {
                // trace( "NO MATCH. head="+head)
                loop( tail, spec, elisions )
              }

              case Nil => elisions
            }
          }

          val elisions = loop( 
            xform.transform( o, spec ).asJsObject.fields.toList, 
            spec, 
            Vector.empty 
          )
          JsObject( elisions.toMap )
        }

        // case a: JsArray => trace.block( "elide-ARRAY("+spec+")" ) { new JsArray( a.elements.map( elide( _, spec ) ) ) }
        // case json => trace.block( "elide-PRIMITIVE("+spec+")" ) { json }
        case a: JsArray => new JsArray( a.elements.map( elide( _, spec ) ) )
        case json => json 
      }
    }
  }
}

object JsonReducable {
  lazy val trace = peds.commons.log.Trace[JsonReducable]


  trait Searchable[+J] extends Any {
    type SimpleResult
    type RecursiveResult

    def \( fieldName: String ): SimpleResult
    def \\( fieldName: String ): RecursiveResult
  }


  implicit class SearchableJsValue( val underlying: JsValue ) extends AnyVal with Searchable[JsValue] {
    type SimpleResult = JsValue
    type RecursiveResult = Seq[JsValue]

    override def \( fieldName: String ): SimpleResult = trace.block( """#### JsValue.\ ####""" ) {JsString( s"""'$fieldName' is undefined on object: $this""" ) }
    override def \\( fieldName: String ): RecursiveResult = trace.block( """#### JsValue.\\ ####""" ) {Nil}
  }


  implicit class SearchableJsObject( val underlying: JsObject ) extends AnyVal with Searchable[JsObject] {
    type SimpleResult = JsValue
    type RecursiveResult = Seq[JsValue]

    override def \( fieldName: String ): SimpleResult = trace.block( """#### JsObject.\ ####""" ) {underlying.fields.get( fieldName ).getOrElse( 
          JsString( s"""'$fieldName' is undefined on object: $this""" )
        )}

    override def \\( fieldName: String ): RecursiveResult = trace.block( """#### JsObject.\\ ####""" ) {
      underlying.fields.foldLeft( Seq[JsValue]() ){ (o, pair) => 
        pair match {
          case (key, value: JsObject) if key == fieldName => o ++ ( value +: ( value \\ fieldName ) )
          case (key, value: JsArray) if key == fieldName => o ++ ( value +: ( value \\ fieldName ) )
          case (_, value: JsObject) => o ++ (value \\ fieldName)
          case (_, value: JsArray) => o ++ (value \\ fieldName)
          case _ => o :+ JsString( s"""'$fieldName' is undefined on object: $this""" )
        }
      }
    }
  }

  implicit class SearchableJsArray( val underlying: JsArray ) extends AnyVal with Searchable[JsArray] {
    type SimpleResult = JsValue
    type RecursiveResult = Seq[JsValue]

    override def \( fieldName: String ): SimpleResult = trace.block( """#### JsArray.\ ####""" ) {JsString( s"""'$fieldName' is undefined on object: $this""" ) }

    override def \\( fieldName: String ): RecursiveResult = trace.block( """#### JsArray.\\ ####""" ) {underlying.elements.flatMap{ 
          case o: JsObject => o \\ fieldName 
          case a: JsArray => a \\ fieldName
          case _ => Seq( JsString( s"""'$fieldName' is undefined on object: $this""" ) )
        }}
  }

  // type JsField = (String, JsValue)

  case object JsonTransformable extends Transformable[JsValue] {
    import Transformable._
    lazy val trace = peds.commons.log.Trace( "JsonTransformable" )

    override def execute = sort

    def sort: PartialFunction[(JsValue, ElemPropValue), JsValue] = {
      case (data: JsObject, epv) if epv._2 == "sort-asc" => sortByEpv( data, epv )( JsValueOrdering )
      case (data: JsObject, epv) if epv._2 == "sort-desc" => sortByEpv( data, epv )( JsValueOrdering.reverse )
    }

    private def sortByEpv( data: JsObject, epv: ElemPropValue )( implicit ord: Ordering[JsValue] ): JsObject = trace.block( "sortByEpv" ) {
      // trace( "data="+data )
      // trace( "epv="+epv )

      val (incl, excl) = data.fields.partition( _._1 == epv._1 )
      // trace( "incl data="+incl )

      val xIncl = for ( (prop, value) <- incl  ) yield {
        // trace( prop+"="+value )
        if ( prop == epv._1 ) trace.block( "%%%%% WATCH %%%%%" ) {
          val result = value match {
            // case v: JsArray => trace(s"JsArray[${v.elements.size}]");new JsArray( v.elements sortBy { c => trace.block("""array.\"""){c \ epv._3} } )
            case v: JsArray => {
              val elems = v.elements sortBy { c => 
                c match {
                  case co: JsObject => co.fields.get( epv._3 ).getOrElse( JsNull )
                  case _ => JsNull
                }
              } 
              new JsArray( elems )
            }

            case o: JsObject => trace(s"JsObject${o.fields.size}");o

            case j => trace("no match");j
          }
trace( "IN LOOP" )
          prop -> result
        }
        else prop -> value
      }

      JsObject( ( excl.toSeq ++ xIncl ).toMap )
    }

    object JsValueOrdering extends Ordering[JsValue] {
      override def compare( lhs: JsValue, rhs: JsValue ): Int = (lhs, rhs) match {
        case (l: JsString, r: JsString) => l.value compareTo r.value
        case (l: JsNumber, r: JsNumber) => l.value compare r.value
        case (l: JsBoolean, r: JsBoolean) => l.value compareTo r.value
        case _ => -1
      }
    }
  }
}
