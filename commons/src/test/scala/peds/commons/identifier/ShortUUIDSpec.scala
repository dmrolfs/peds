package peds.commons.identifier

import com.eaio.uuid.UUID
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import com.typesafe.scalalogging.LazyLogging


class ShortUUIDSpec extends FlatSpec with Matchers with LazyLogging {

  "A ShortUUID" should "create a nil" in {
    ShortUUID.short2UUID( ShortUUID.nilUUID ) shouldBe UUID.nilUUID
  }

  it should "represent uuid without loss" in {
    val expectedUuid = new UUID
    val short = ShortUUID.uuidToShort( expectedUuid )
    val replayedUuid = ShortUUID.short2UUID( short )
    expectedUuid shouldBe replayedUuid
  }
  
  it should "convert from uuid implicitly" in {
    val uuid = new UUID
    val expected = ShortUUID.uuidToShort( uuid )
    val actual: ShortUUID = uuid
    expected shouldBe actual
  }
  
  it should "convert to uuid implicitly" in {
    val expected = new UUID
    val short = ShortUUID.uuidToShort( expected )
    val actual: UUID = short
    expected shouldBe actual
  }

  it should "create unique short uuids" in {
    val first = ShortUUID()
    val second = ShortUUID()
    first should not be second
  }
}