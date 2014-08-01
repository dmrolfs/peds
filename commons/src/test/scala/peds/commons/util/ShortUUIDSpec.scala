package peds.commons.util

import com.eaio.uuid.UUID
import org.specs2._
import com.typesafe.scalalogging.LazyLogging


class ShortUUIDSpec extends mutable.Specification with LazyLogging {

  "A ShortUUID" should {
    "create a nil" in {
      ShortUUID.short2UUID( ShortUUID.nilUUID ) must_== UUID.nilUUID
    }

    "represent uuid without loss" in {
      val expectedUuid = new UUID
      val short = ShortUUID.uuidToShort( expectedUuid )
      val replayedUuid = ShortUUID.short2UUID( short )
      expectedUuid must_== replayedUuid
    }
    
    "convert from uuid implicitly" in {
      val uuid = new UUID
      val expected = ShortUUID.uuidToShort( uuid )
      val actual: ShortUUID = uuid
      expected must_== actual
    }
    
    "convert to uuid implicitly" in {
      val expected = new UUID
      val short = ShortUUID.uuidToShort( expected )
      val actual: UUID = short
      expected must_== actual
    }

    "create unique short uuids" in {
      val first = ShortUUID()
      val second = ShortUUID()
      first must_!= second
    }
  }
}