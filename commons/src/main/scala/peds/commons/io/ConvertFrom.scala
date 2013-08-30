package peds.commons.io

import scala.annotation.implicitNotFound
import scala.reflect.ClassTag
import peds.commons.log.Trace


/** A base trait for conversion factories.
 *
 *  @tparam From  the type of the source format that requests
 *                a conversion.
 *  @tparam To    the type of object to be created.
 *
 *  @see Builder
 */
// @deprecated( "want to replace with a more complete type class framework (eg, via play or spray) or even scala-pickling", "7/1/2013" )
@implicitNotFound(msg = "Cannot convert an instance of type ${To} based on a source of type ${From}.")
trait ConvertFrom[-From, +To] extends Function1[From, To]

object ConvertFrom {
  val trace = Trace[ConvertFrom.type]

  type Conversion = (Class[_], Class[_])

  private var myConversions: Map[Conversion, ConvertFrom[_,_]] = Map.empty
  def conversions: Set[Conversion] = myConversions.keySet

  def register[From, To]( cf: ConvertFrom[From, To] )( implicit mFrom: ClassTag[From], mTo: ClassTag[To] ): Unit = {
    import scala.language.existentials

    val k = mFrom.runtimeClass -> mTo.runtimeClass
    myConversions += k -> cf
    trace( "registering " + k )
  }

  def convert[From, To]( source: From )( implicit mFrom: ClassTag[From], mTo: ClassTag[To] ): To = trace.block("convert") {
    import scala.language.existentials
    
    val k = mFrom.runtimeClass -> mTo.runtimeClass
    trace( "key="+k )
    val c = myConversions( k ).asInstanceOf[ConvertFrom[From, To]]
    c( source )
  }
}