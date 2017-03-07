package omnibus.slick

import java.sql.Timestamp
import scala.slick.lifted.MappedTypeMapper.base
import scala.slick.lifted.TypeMapper
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.DateTimeZone.UTC
import _root_.spray.json._


trait CommonTypeMappers {
  implicit val DateTimeMapper: TypeMapper[DateTime] = base[DateTime, Timestamp](
    d => new Timestamp( d.getMillis ), 
    t => new DateTime( t.getTime, UTC )
  )

  implicit val DateTimeZoneMapper: TypeMapper[DateTimeZone] = base[DateTimeZone, String](
    d => d.getID, 
    s => DateTimeZone forID s
  )

  implicit val JsonMapper: TypeMapper[JsValue] = base[JsValue, String](
    j => j.compactPrint, 
    s => s.asJson
  )
}