package omnibus.identifier
import org.scalatest.{ FlatSpec, Matchers }
import scribe.Level
import io.jvm.uuid._

class ShortUUIDSpec extends FlatSpec with Matchers {
  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  "A ShortUUID" should "create a nil" in {
    ShortUUID.toUUID( ShortUUID.zero ) shouldBe UUID( 0L, 0L )
  }

  it should "represent uuid without loss" in {
    val expectedUuid = UUID.random
    val short = ShortUUID.fromUUID( expectedUuid )
    val replayedUuid = ShortUUID.toUUID( short )
    expectedUuid shouldBe replayedUuid
  }

  it should "convert from uuid implicitly" in {
    val uuid = UUID.random
    val expected = ShortUUID.fromUUID( uuid )
    val actual: ShortUUID = uuid
    expected shouldBe actual
  }

  it should "convert to uuid implicitly" in {
    val expected = UUID.random
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
