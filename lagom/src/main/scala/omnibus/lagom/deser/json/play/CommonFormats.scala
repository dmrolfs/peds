package omnibus.lagom.deser.json.play

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}
import play.api.libs.json._
import journal._

object CommonFormats {

  private val log = Logger[CommonFormats.type]

  //  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = Reads {
  //    case JsString(s) =>
  //      try {
  //        JsSuccess(enum.withName(s).asInstanceOf[E#Value])
  //      } catch {
  //        case _: NoSuchElementException =>
  //          JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
  //      }
  //    case _ => JsError("String value expected")
  //  }
  //  def enumWrites[E <: Enumeration]: Writes[E#Value] = Writes(v => JsString(v.toString))
  //  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
  //    Format(enumReads(enum), enumWrites)
  //  }

  implicit val timeunitFormat: Format[TimeUnit] = new Format[TimeUnit] {
    override def reads( json: JsValue ): JsResult[TimeUnit] = {
      Try {
        TimeUnit.valueOf( json.as[String] )
      }.fold(
        ex => JsError( JsonValidationError( ex.getMessage ) ),
        JsSuccess( _ )
      )
    }

    override def writes( tu: TimeUnit ): JsValue = {
      Json.toJson( tu.name )
    }
  }

  implicit val finiteDurationFormat: Format[FiniteDuration] = new Format[FiniteDuration] {
    override def reads( json: JsValue ): JsResult[FiniteDuration] = {
      Try {
        FiniteDuration(
          length = (json \ "length").as[Long],
          unit = (json \ "unit").as[TimeUnit]
        )
      }.fold(
        ex => JsError( JsonValidationError( ex.getMessage ) ),
        JsSuccess( _ )
      )
    }

    override def writes( d: FiniteDuration ): JsValue = {
      Json.obj(
        ("length" -> Json.toJson( d.length )),
        ("unit"   -> Json.toJson( d.unit ))
      )
    }
  }

  def singletonReads[O]( singleton: O ): Reads[O] = {
    (__ \ "value")
      .read[String]
      .collect(
        JsonValidationError(
          s"Expected a JSON object with a single field with key 'value' and value '${singleton.getClass.getSimpleName}'"
        )
      ) {
        case s if s == singleton.getClass.getSimpleName => singleton
      }
  }

  def singletonWrites[O]: Writes[O] = Writes { singleton =>
    Json.obj( "value" -> singleton.getClass.getSimpleName )
  }

  def singletonFormat[O]( singleton: O ): Format[O] = {
    Format( singletonReads( singleton ), singletonWrites )
  }

  //todo find proper json format
  implicit val throwableFormat: Format[Throwable] = new Format[Throwable] {
    override def reads( json: JsValue ): JsResult[Throwable] = {
      val result = Try {
        val classname = (json \ "type").as[String]
        val clazz = getClass.getClassLoader.loadClass( classname )
        val message = (json \ "message").as[String]
        val cause = (json \ "cause").asOpt[Throwable]

        cause
          .map { ex =>
            val ctor = clazz.getDeclaredConstructor( classOf[String], classOf[Throwable] )
            ctor.newInstance( message, ex )
          }
          .getOrElse {
            val ctor = clazz.getDeclaredConstructor( classOf[String] )
            ctor.newInstance( message )
          }
      }

      result match {
        case Success( t )  => JsSuccess[Throwable]( t.asInstanceOf[Throwable] )
        case Failure( ex ) => JsError( ex.getMessage )
      }
    }

    override def writes( o: Throwable ): JsValue = {
      log.debug( s"throwableFormat.writes: o=${o}" )
      log.debug( s"throwableFormat.writes: o.class=${o.getClass}" )
      log.debug( s"throwableFormat.writes: o.class.name=${o.getClass.getName}" )
      log.debug( s"throwableFormat.writes: o.message=${o.getMessage}" )
      log.debug( s"throwableFormat.writes: o.cause=${o.getCause}" )

      import play.api.libs.json.Json.JsValueWrapper
      val fields: Seq[Option[( String, JsValueWrapper )]] = Seq(
        Some( ("type"    -> o.getClass.getName) ),
        Some( ("message" -> o.getMessage) ),
        Option( o.getCause ).map( ex => ("cause" -> ex) )
      )
      Json.obj( fields.flatten: _* )
    }
  }

  implicit val uuidFormat: Format[UUID] = new Format[UUID] {
    override def writes( o: UUID ): JsValue = Json.toJson( o.toString )
    override def reads( json: JsValue ): JsResult[UUID] = {
      Try {
        UUID.fromString( json.as[String] )
      }.fold(
          ex => JsError( JsonValidationError( ex.getMessage ) ),
          uuid => JsSuccess( uuid )
        )
    }
    //  implicit val uuidReads: Reads[UUID] = {
    //    implicitly[Reads[String]]
    //      .collect(
    //        JsonValidationError( "Invalid UUID" )
    //      )(
    //        Function.unlift { str =>
    //          Try( UUID.fromString( str ) ).toOption
    //        }
    //      )
    //  }
    //
    //  implicit val uuidWrites: Writes[UUID] = Writes { uuid =>
    //    JsString( uuid.toString )
    //  }
  }
}
