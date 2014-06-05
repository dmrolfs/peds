/*
 * Copyright (C) 2013 Damon Rolfs.  All rights reserved.
 * Heavily based on code copyright (C) 2013 by CakeSolutions.  All rights reserved.
 * see https://github.com/janm399/scalad/tree/master/src/main/scala/org/eigengo/scalad/mongo/sprayjson
 *
 * This product may contain certain open source code licensed to Damon Rolfs.
 * Please open the license.txt file, read the contents and abide by the terms of all
 * such open source licenses.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package peds.spray

import java.net.URI
import java.util.{Date, Locale}
import java.util.{UUID => JUUID}
import com.eaio.uuid.{UUID => EUUID}
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.{CollectingMarshallingContext, MetaMarshallers, Marshaller}
import spray.http.{HttpEntity, StatusCode}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import peds.commons.util._
import peds.spray.domain._


trait EndpointMarshalling 
  extends MetaMarshallers 
  with SprayJsonSupport 
  with JodaMarshalling
  with I18nMarshalling
  with EitherErrorMarshalling


trait JodaMarshalling {
  implicit object DateTimeJsonFormat extends JsonFormat[DateTime] {
    override def write( d: DateTime ): JsValue = JsString( d.toString() )
    override def read( value: JsValue ): DateTime = value match {
      case JsString( s ) => DateTime parse s
      case x => deserializationError( "Expected DateTime as JsString, but got " + x )
    }
  }

  implicit object LocalDateJsonFormat extends JsonFormat[LocalDate] {
    override def write( d: LocalDate ): JsValue = JsString( d.toString )
    override def read( value: JsValue ): LocalDate = value match {
      case JsString( s ) => LocalDate parse s
      case x => deserializationError( "Expected LocalDate as JsString, but got " + x )
    }
  }

  implicit object DateTimeZoneJsonFormat extends JsonFormat[DateTimeZone] {
    override def write( tz: DateTimeZone ): JsValue = JsString( tz.getID )
    override def read( value: JsValue ): DateTimeZone = value match {
      case JsString( s ) => DateTimeZone forID s
      case x => deserializationError( "Expected DateTimeZone as JsString, but got " + x )
    }
  }
}

trait I18nMarshalling {
  implicit object LocaleJsonFormat extends JsonFormat[Locale] {
    override def write( l: Locale ): JsValue = JsString( l.toLanguageTag )
    override def read( value: JsValue ): Locale = value match {
      case JsString( s ) => Locale forLanguageTag s
      case x => deserializationError( "Expected Locale as JsString, but got " + x )
    }
  }
}

case class ErrorResponseException( responseStatus: StatusCode, response: Option[HttpEntity] ) extends Exception


trait EitherErrorMarshalling {

  /**
   * Marshaller that uses some ``ErrorSelector`` for the value on the left to indicate that it is an error, even though
   * the error response should still be marshalled and returned to the caller.
   *
   * This is useful when you need to return validation or other processing errors, but need a bit more information than
   * just ``HTTP status 422`` (or, even worse simply ``400``).
   *
   * Bring an implicit instance of this method into scope of your HttpServices to get the status code.
   *
   * @param status the status code to return for errors.
   * @param ma the marshaller for ``A`` (the error)
   * @param mb the marshaller for ``B`` (the success)
   * @tparam A the type on the left
   * @tparam B the type on the right
   * @return the marshaller instance
   */
  def errorSelectingEitherMarshaller[A, B]( status: StatusCode )( implicit ma: Marshaller[A], mb: Marshaller[B] ) = {
    Marshaller[Either[A, B]] { (value, ctx) =>
      value match {
        case Left(a) =>
          val mc = new CollectingMarshallingContext()
          ma(a, mc)
          ctx.handleError(ErrorResponseException(status, mc.entity))
        case Right(b) =>
          mb(b, ctx)
      }
    }
  }
}


/** Convenient implicit conversions */
object BigNumberMarshalling {

  import language.implicitConversions

  implicit def StringBigDecimalToBigDecimal(value: StringBigDecimal) = value.value

  implicit def StringBigIntBigDecimal(value: StringBigInt) = value.value

  implicit def StringToStringBigDecimal(value: String) = StringBigDecimal(value)

  implicit def StringToStringBigInt(value: String) = StringBigInt(value)

  implicit def IntToStringBigDecimal(value: Int) = StringBigDecimal(BigDecimal(value))

  implicit def IntToStringBigInt(value: Int) = StringBigInt(BigInt(value))

  implicit def BigDecimalToStringBigDecimal(value: BigDecimal) = StringBigDecimal(value)

  implicit def BigIntToStringBigInt(value: BigInt) = StringBigInt(value)
}

/** Alternative to [[spray.json.BasicFormats]] `JsNumber` marshalling. */
trait BigNumberMarshalling {

  implicit object StringBigDecimalJsonFormat extends RootJsonFormat[StringBigDecimal] {
    def write(obj: StringBigDecimal) = JsString(obj.value.toString())

    def read(json: JsValue) = json match {
      case JsString(value) => StringBigDecimal(value)
      case _ => deserializationError("Expected String for StringBigDecimal")
    }
  }

  implicit object StringBigIntJsonFormat extends RootJsonFormat[StringBigInt] {

    def write(obj: StringBigInt) = JsString(obj.value.toString())

    def read(json: JsValue) = json match {
      case JsString(value) => StringBigInt(value)
      case _ => deserializationError("Expected String for StringBigInt")
    }
  }

}

trait DateMarshalling {

  implicit object DateJsonFormat extends BsonMarshalling[Date] with IsoDateChecker {

    override val key = "$date"

    override def writeString(obj: Date) = dateToIsoString(obj)

    override def readString(value: String) = parseIsoDateString(value) match {
      case None => deserializationError("Expected ISO Date format, got %s" format (value))
      case Some(date) => date
    }
  }

}

/** [[scala.math.BigDecimal]] wrapper that is marshalled to `String`
  * and can therefore be persisted into MongoDB */
final case class StringBigDecimal(value: BigDecimal)

object StringBigDecimal {
  def apply(value: String) = new StringBigDecimal(BigDecimal(value))
}

/** [[scala.math.BigInt]] wrapper that is marshalled to `String`
  * and can therefore be persisted into MongoDB */
final case class StringBigInt(value: BigInt)

object StringBigInt {
  def apply(value: String) = new StringBigInt(BigInt(value))
}

/** Allows special types to be marshalled into a meta JSON language
  * which allows ScalaD Mongo serialisation to convert into the correct
  * BSON representation for database persistence.
  */
trait BsonMarshalling[T] extends RootJsonFormat[T] {

  val key: String

  def writeString(obj: T): String

  def readString(value: String): T

  def write(obj: T) = JsObject(key -> JsString(writeString(obj)))

  def read(json: JsValue) = json match {
    case JsObject(map) => map.get(key) match {
      case Some(JsString(text)) => readString(text)
      case x => deserializationError("Expected %s, got %s" format(key, x))
    }
    case x => deserializationError("Expected JsObject, got %s" format (x))
  }

}

trait UuidMarshalling {

  implicit object EaioUuidJsonFormat extends BsonMarshalling[EUUID] {
    override val key = "$uuid"

    override def writeString( obj: EUUID ): String = obj.toString

    override def readString( value: String ): EUUID = new EUUID( value )
  }

  implicit object JavaUuidJsonFormat extends BsonMarshalling[JUUID] with JUuidChecker {

    override val key = "$uuid"

    override def writeString( obj: JUUID ): String = obj.toString

    override def readString( value: String ): JUUID = parseUuidString(value) match {
      case None => deserializationError("Expected UUID format, got %s" format (value))
      case Some( uuid ) => uuid
    }
  }

}


trait UriMarshalling {

  implicit protected object UriJsonFormat extends RootJsonFormat[URI] {
    def write(x: URI) = JsString(x toString())

    def read(value: JsValue) = value match {
      case JsString(x) => new URI(x)
      case x => deserializationError("Expected URI as JsString, but got " + x)
    }
  }
}


/**
 * Flattens the JSON representation of a case class that contains a single `value`
 * element from:
 *
 * {"value": "..."}
 *
 * to `"..."`
 */
case class SingleValueCaseClassFormat[T <: {def value : V}, V](construct: V => T)(implicit delegate: JsonFormat[V]) extends RootJsonFormat[T] {

  import scala.language.reflectiveCalls
  override def write(obj: T) = delegate.write(obj.value)

  override def read(json: JsValue) = construct(delegate.read(json))
}


// Marshaller for innocent case classes that don't have any parameters
// assumes that the case classes behave like singletons
// https://github.com/spray/spray-json/issues/41
case class NoParamCaseClassFormat[T](instance: T) extends RootJsonFormat[T] {

  override def write(obj: T) = JsString(instance.getClass.safeSimpleName)

  override def read(json: JsValue) = json match {
    case JsString(x) =>
      if(x != instance.getClass.safeSimpleName)
        deserializationError("Expected %s, but got %s" format (instance.getClass.safeSimpleName, x))
      instance
    case x => deserializationError("Expected JsString, but got " + x)
  }
}

// nulls are used in some Mongo Queries, so don't forget to import this
trait NullMarshalling {
  implicit protected val NullFormat = new RootJsonFormat[Null] {
    def write(obj: Null) = JsNull
    def read(json: JsValue) = null
  }
}