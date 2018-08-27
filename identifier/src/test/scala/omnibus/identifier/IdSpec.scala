package omnibus.identifier

import org.scalatest.{ Matchers, WordSpec }
import scribe.Level

class IdSpec extends WordSpec with Matchers {
  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  case class Foo( id: Foo#TID, f: String ) {
    type TID = Foo.identifying.TID
  }

  object Foo {
    def nextId: Foo#TID = identifying.next
    implicit val identifying = new Identifying.ByShortUuid[Foo]
  }

  case class Bar( id: Id[Bar], b: Double )

  object Bar {
    def nextId: Id.Aux[Bar, Long] = identifying.next
    implicit val identifying = new Identifying.ByLong[Bar]
  }

  "An Id" should {
    "summons Aux" in {
      implicit val fid = Foo.identifying.next
      val fa: Id.Aux[Foo, ShortUUID] = Id[Foo]
      ShortUUID.zero shouldBe a[fa.IdType]

      implicit val bid = Bar.nextId
      val ba: Id.Aux[Bar, Long] = Id[Bar]
      bid.value.getClass shouldBe classOf[Long] //todo: better handle primitive boxing
    }

    "create Id of varying types" in {
//      import Foo._

      val suid = ShortUUID()
      val fid: Id[Foo] = Id of suid
      fid shouldBe a[Id[Foo]]
      fid.toString shouldBe s"FooId(${suid})"
      fid.value shouldBe a[ShortUUID]
      fid.value shouldBe suid

      fid.value shouldBe suid
      suid shouldBe fid.value

      val bid: Id[Bar] = Id of 13L
      bid shouldBe a[Id[Bar]]
      bid.toString shouldBe "BarId(13)"
      bid.value.getClass shouldBe classOf[java.lang.Long]
      bid.value shouldBe 13L
    }

    "invalid id type should fail" in {
      "val fid: Id[Foo] = Id of 17L" shouldNot compile
    }

    "create Id from strings" in {
      val fid = ShortUUID()
      val frep = fid.toString

      val f: Id[Foo] = Id fromString frep
      f shouldBe a[Id[Foo]]
      f.toString shouldBe s"FooId(${fid})"
      f.value shouldBe a[ShortUUID]
      f.value shouldBe fid

      val bid = 17L
      val brep = bid.toString
      val b: Id[Bar] = Id fromString brep
      b shouldBe a[Id[Bar]]
      b.toString shouldBe s"BarId(${bid})"
      b.value.getClass shouldBe classOf[java.lang.Long]
      b.value shouldBe bid
    }

    "invalid id rep type should fail" in {
      val fid = 17L
      val frep = fid.toString
      an[IllegalArgumentException] should be thrownBy Id.fromString[Foo, ShortUUID]( frep )
    }

    "custom labeling can override class label" in {
      implicit val fooLabeling = new CustomLabeling[Foo] {
        override def label: String = "SPECIAL_FOO"
      }

      val suid = ShortUUID()
      val fid = Id.of[Foo, ShortUUID]( suid )
      fid.toString shouldBe s"SPECIAL_FOO(${suid})"

      implicit val barLabeling = new EmptyLabel[Bar]
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
  }
}
