package omnibus.commons.identifier

import com.eaio.uuid.UUID
import org.scalatest._
import org.scalatest.Matchers

class ShortUUIDSpec extends FlatSpec with Matchers {

  "A ShortUUID" should "create a nil" in {
    ShortUUID.toUUID( ShortUUID.zero ) shouldBe UUID.nilUUID
  }

  it should "represent uuid without loss" in {
    val expectedUuid = new UUID
    val short = ShortUUID.fromUUID( expectedUuid )
    val replayedUuid = ShortUUID.toUUID( short )
    expectedUuid shouldBe replayedUuid
  }

  it should "convert from uuid implicitly" in {
    val uuid = new UUID
    val expected = ShortUUID.fromUUID( uuid )
    val actual: ShortUUID = uuid
    expected shouldBe actual
  }

  it should "convert to uuid implicitly" in {
    val expected = new UUID
    val short = ShortUUID.fromUUID( expected )
    val actual: UUID = short
    expected shouldBe actual
  }

  it should "create unique short uuids" in {
    val first = ShortUUID()
    val second = ShortUUID()
    first should not be second
  }
}
