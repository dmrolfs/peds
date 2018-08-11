package omnibus.commons.partial

import scala.annotation.tailrec
import org.json4s._

trait JsonReducable {
  implicit def jsonReducable( implicit xform: Transformable[JValue] ): Reducable[JValue] =
    new Reducable[JValue] {
      override def elide( data: JValue, spec: PartialCriteria ): JValue = {
        data match {
          case o: JObject =>
            /*trace.block( "elide-OBJECT("+spec+")" )*/
            {
              @tailrec def loop(
                nodes: List[JField],
                spec: PartialCriteria,
                partials: Vector[JField]
              ): Vector[JField] = {
                // trace( "loop-head="+nodes.headOption.getOrElse("{}") )
                // trace( "loop-spec="+spec )
                // trace( "loop-partials="+partials )
                nodes match {
                  case head :: tail if spec.contains( head._1 ) => {
                    // trace( "head MATCH spec. head="+head )
                    val nextSpec = spec.get( head._1 ).getOrElse( spec )
                    loop( tail, spec, partials :+ (head._1 -> elide( head._2, nextSpec )) )
                  }

                  case _ :: tail => {
                    // trace( "NO MATCH. head="+head)
                    loop( tail, spec, partials )
                  }

                  case Nil => partials
                }
              }

              val partials = loop(
                xform.transform( o, spec ).asInstanceOf[JObject].obj,
                spec,
                Vector.empty
              )
              JObject( partials: _* )
            }

          // case a: JArray => trace.block( "elide-ARRAY("+spec+")" ) { new JArray( a.elements.map( elide( _, spec ) ) ) }
          // case json => trace.block( "elide-PRIMITIVE("+spec+")" ) { json }
          case a: JArray => JArray( a.arr map { elide( _, spec ) } )
          case json      => json
        }
      }
    }
}

object JsonReducable {

  trait Searchable[+J] extends Any {
    type SimpleResult
    type RecursiveResult

    def \( fieldName: String ): SimpleResult
    def \\( fieldName: String ): RecursiveResult
  }

  implicit class SearchableJValue( val underlying: JValue ) extends AnyVal with Searchable[JValue] {
    type SimpleResult = JValue
    type RecursiveResult = Seq[JValue]

    override def \( fieldName: String ): SimpleResult =
      JString( s"""'$fieldName' is undefined on object: $this""" )
    override def \\( fieldName: String ): RecursiveResult = Nil
  }

  implicit class SearchableJObject( val underlying: JObject )
      extends AnyVal
      with Searchable[JObject] {
    type SimpleResult = JValue
    type RecursiveResult = Seq[JValue]

    override def \( fieldName: String ): SimpleResult = {
      underlying.obj find { _._1 == fieldName } map { _._2 } getOrElse {
        JNothing /*JString( s"""'$fieldName' is undefined on object: $this""" )*/
      }
    }

    override def \\( fieldName: String ): RecursiveResult = {
      underlying.obj.foldLeft( Seq[JValue]() ) { ( o, nameValue ) =>
        nameValue match {
          case ( key, value: JObject ) if key == fieldName => o ++ (value +: (value \\ fieldName))
          case ( key, value: JArray ) if key == fieldName  => o ++ (value +: (value \\ fieldName))
          case ( _, value: JObject )                       => o ++ (value \\ fieldName)
          case ( _, value: JArray )                        => o ++ (value \\ fieldName)
          case _                                           => o :+ JNothing /*JString( s"""'$fieldName' is undefined on object: $this""" )*/
        }
      }
    }
  }

  implicit class SearchableJArray( val underlying: JArray ) extends AnyVal with Searchable[JArray] {
    type SimpleResult = JValue
    type RecursiveResult = Seq[JValue]

    override def \( fieldName: String ): SimpleResult =
      JString( s"""'$fieldName' is undefined on object: $this""" )

    override def \\( fieldName: String ): RecursiveResult = {
      underlying.arr flatMap {
        case o: JObject => o \\ fieldName
        case a: JArray  => a \\ fieldName
        case _          => Seq( JNothing /* JString( s"""'$fieldName' is undefined on object: $this""" )*/ )
      }
    }
  }

  // type JsField = (String, JValue)

  case object JsonTransformable extends Transformable[JValue] {
    import Transformable._

    override def execute = sort

    def sort: PartialFunction[( JValue, ElemPropValue ), JValue] = {
      case ( data: JObject, epv ) if epv._2 == "sort-asc" =>
        sortByEpv( data, epv )( JValueOrdering )
      case ( data: JObject, epv ) if epv._2 == "sort-desc" =>
        sortByEpv( data, epv )( JValueOrdering.reverse )
    }

    private def sortByEpv( data: JObject, epv: ElemPropValue )(
      implicit ord: Ordering[JValue]
    ): JObject = {
      // trace( "data="+data )
      // trace( "epv="+epv )

      val ( incl, excl ) = data.obj.partition( _._1 == epv._1 )
      // trace( "incl data="+incl )

      val xIncl = for (( prop, value ) <- incl) yield {
        // trace( prop+"="+value )
        if (prop == epv._1) {
          val result = value match {
            // case v: JArray => trace(s"JArray[${v.elements.size}]");new JArray( v.elements sortBy { c => trace.block("""array.\"""){c \ epv._3} } )
            case v: JArray => {
              val elems = v.arr sortBy { c =>
                c match {
                  case co: JObject => co.obj find { _._1 == epv._3 } map { _._2 } getOrElse JNull
                  case _           => JNull
                }
              }
              new JArray( elems )
            }

            case o: JObject => o

            case j => j
          }
          prop -> result
        } else prop -> value
      }

      JObject( (excl.toSeq ++ xIncl): _* )
    }

    object JValueOrdering extends Ordering[JValue] {
      override def compare( lhs: JValue, rhs: JValue ): Int = ( lhs, rhs ) match {
        case ( l: JString, r: JString )   => l.s compareTo r.s
        case ( l: JBool, r: JBool )       => l.value compareTo r.value
        case ( l: JDouble, r: JDouble )   => l.num compare r.num
        case ( l: JDecimal, r: JDecimal ) => l.num compare r.num
        case ( l: JInt, r: JInt )         => l.num compare r.num
        case _                            => -1
      }
    }
  }
}
