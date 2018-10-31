package omnibus.identifier

import scala.language.existentials
import scala.util.Try
import org.scalatest.{ EitherValues, Matchers, Tag, WordSpec }
import com.softwaremill.id.pretty.IdPrettifier
import journal._
import io.jvm.uuid.UUID
import omnibus.core.{ AllErrorsOr, AllIssuesOr, ErrorOr }

class IdentifyingSpec extends WordSpec with Matchers with EitherValues {
  private val log = Logger[IdentifyingSpec]

  case class Foo( id: Id[Foo], f: String )

  object Foo {
    type TID = identifying.TID
    def nextId: TID = identifying.next
    implicit val identifying = Identifying.byShortUuid[Foo]
  }

  case class Bar( id: Id[Bar], b: Double )

  object Bar {
    def nextId: Id[Bar] = identifying.next
    implicit val labeling = Labeling.custom[Bar]( "SuperBar" )
    implicit val identifying = new Identifying.ByLong[Bar]
  }

  case class Zed( id: Zed.TID, score: Double )

  object Zed {
    type TID = identifying.TID
    implicit val labeling = Labeling.empty[Zed]
    implicit val identifying = Identifying.byUuid[Zed]
  }

  type OZed = Option[Zed]

  case class Snowflake( id: Snowflake.TID, points: Int )

  object Snowflake {
    type TID = identifying.TID
    implicit val identifying = Identifying.bySnowflake[Snowflake]()
  }

  object WIP extends Tag( "wip" )

  "An Identifying" should {
    "summons Aux" in {
      val fa: Identifying.Aux[Foo, ShortUUID] = Identifying[Foo]
      ShortUUID.zero shouldBe a[fa.ID]
    }

    "identifying should work with optional state entity types" taggedAs WIP in {
      def makeOptionZedId()( implicit i: Identifying[OZed] ): i.TID = i.next
      def makeOptionZedIdAux()( implicit i: Identifying.Aux[Option[Zed], UUID] ): i.TID = i.next

      val ozid = makeOptionZedId()
      log.info( s"ozid = ${ozid}" )
      "Zed( id = makeOptionZedId(), score = 3.14 )" should compile

      val ozidAux = makeOptionZedIdAux()
      log.info( s"ozidAux = ${ozidAux}" )
      "val z: Id.Aux[Zed, UUID] = ozidAux" should compile
      "Zed( id = makeOptionZedIdAux(), score = 3.14 )" should compile
    }

    "provide label" in {
      Foo.identifying.label shouldBe "Foo"
      Bar.identifying.label shouldBe "SuperBar"
      Zed.identifying.label shouldBe empty
    }

    "support conversion to another entity basis" in {
      val fid: Identifying.Aux[Foo, ShortUUID] = Identifying[Foo]
      val bid: Identifying.Aux[Bar, ShortUUID] = fid.as[Bar]
      bid.zeroValue shouldBe ShortUUID.zero
      bid.label shouldBe "SuperBar"
    }

    "option identifying is derived from underlying type" in {
      val fooIdentifying = Identifying[Foo]
      val oFooIdentifying = Identifying[Option[Foo]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "try identifying is derived from underlying type" in {
      val fooIdentifying = Identifying[Foo]
      val oFooIdentifying = Identifying[Try[Foo]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "ErrorOr identifying is derived from underlying type" in {
      val fooIdentifying = Identifying[Foo]
      val oFooIdentifying = Identifying[ErrorOr[Foo]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "AllErrorsOr identifying is derived from underlying type" in {
      val fooIdentifying = Identifying[Foo]
      val oFooIdentifying = Identifying[AllErrorsOr[Foo]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "AllIssuesOr identifying is derived from underlying type" in {
      val fooIdentifying = Identifying[Foo]
      val oFooIdentifying = Identifying[AllIssuesOr[Foo]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "create Id of varying types" in {
      val suid = ShortUUID()
      val fid: Id[Foo] = Id of suid
      fid.toString shouldBe s"FooId(${suid})"
      fid.value shouldBe a[ShortUUID]
      fid.value shouldBe suid

      fid.value shouldBe suid
      suid shouldBe fid.value

      val bid: Id[Bar] = Id of 13L
      bid.toString shouldBe "SuperBarId(13)"
      bid.value.getClass shouldBe classOf[java.lang.Long]
      bid.value shouldBe 13L

      val uuid = UUID.random
      val zid: Id[OZed] = Id of uuid
      zid.toString shouldBe uuid.toString
      zid.value.getClass shouldBe classOf[UUID]
      zid.value shouldBe uuid
    }

    "invalid id type should fail" in {
      "val fid: Id[Foo] = Id of 17L" shouldNot compile
    }

    "create Id from strings" in {
      val fid = ShortUUID()
      val frep = fid.toString

      val f: Id[Foo] = Id fromString frep
      f.toString shouldBe s"FooId(${fid})"
      f.value shouldBe a[ShortUUID]
      f.value shouldBe fid

      val bid = 17L
      val brep = bid.toString
      val b: Id[Bar] = Id fromString brep
      b.toString shouldBe s"SuperBarId(${bid})"
      b.value.getClass shouldBe classOf[java.lang.Long]
      b.value shouldBe bid

      val zid = UUID.random
      val zrep = zid.toString
      val z: Id[Zed] = Id fromString zrep
      z.toString shouldBe zrep
      z.value.getClass shouldBe classOf[UUID]
      z.value shouldBe zid
    }

    "invalid id rep type should fail" in {
      val fid = 17L
      val frep = fid.toString

      an[IllegalArgumentException] should be thrownBy Id.fromString[Foo, ShortUUID]( frep )
    }

    "custom labeling can override class label" in {
      implicit val fooLabeling = Labeling.custom[Foo]( "SPECIAL_FOO_" )

      val suid = ShortUUID()
      val fid = Id.of[Foo, ShortUUID]( suid )
      fid.toString shouldBe s"SPECIAL_FOO_Id(${suid})"

      implicit val barLabeling = new EmptyLabeling[Bar]
      val bid = Id.of[Bar, Long]( 17L )
      bid.toString shouldBe "17"
    }

    "extract id value from Id" in {
      val expected = ShortUUID()
      val fid: Id[Foo] = Id of expected
      val Id( actual ) = fid
      actual shouldBe expected
      actual shouldBe a[ShortUUID]
    }

    "create snowflake ids" in {
      Snowflake.identifying.zero shouldBe Id.fromString[Snowflake, String]( "" )
      val sfIdentifying = Identifying[Snowflake]
      val sid = sfIdentifying.next
      sid.value should not be ("")

      val sid2 = sfIdentifying.next
      sid2 should not be (sid)

      val prettifier = IdPrettifier.default
      prettifier
        .toIdSeed( sid2.value )
        .right
        .get should be > prettifier.toIdSeed( sid.value ).right.get
      sid2.getClass shouldBe sid.getClass
    }
  }
}
