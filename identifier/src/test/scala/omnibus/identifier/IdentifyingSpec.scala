package omnibus.identifier

import scala.util.Try
import omnibus.core.{ AllErrorsOr, AllIssuesOr, ErrorOr }
import org.scalatest.{ Matchers, Tag, WordSpec }
import scribe.Level
import io.jvm.uuid.UUID

class IdentifyingSpec extends WordSpec with Matchers {
  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  case class Foo( id: Id[Foo], f: String )

  object Foo {
    type TID = identifying.TID
    def nextId: TID = identifying.next
    implicit val identifying = Identifying.byShortUuid[Foo]
  }

  case class Bar( id: Id[Bar], b: Double )

  object Bar {
    def nextId: Id[Bar] = identifying.next
    implicit val identifying = new Identifying.ByLong[Bar]
  }

  case class Zed( id: Zed.TID, score: Double )

  object Zed {
    type TID = identifying.TID
    implicit val identifying = Identifying.byUuid[Zed]
  }

  object WIP extends Tag( "wip" )

  "An Identifying" should {
    "summons Aux" in {
      val fa: Identifying.Aux[Foo, ShortUUID] = Identifying[Foo]
      ShortUUID.zero shouldBe a[fa.ID]
    }

    "identifying should work with optional state entity types" taggedAs WIP in {
      def makeOptionZedId()( implicit i: Identifying[Option[Zed]] ): i.TID = i.next
      def makeOptionZedIdAux()( implicit i: Identifying.Aux[Option[Zed], UUID] ): i.TID = i.next
//      def makeOptionZedIdEntityAux()(
//        implicit i: Identifying.Full[Zed, UUID]
//      ): i.TID = i.next
      def makeOptionZedIdFullAux()(
        implicit i: Identifying.FullAux[Option[Zed], Zed, UUID]
      ): i.TID = i.next

//      val o1 = Identifying[Option[Zed]]
//      val oid1 = o1.next
//      val zid: Id[Zed] = oid1
//      val zida: Id.Aux[Zed, UUID] = oid1

//      import scala.language.existentials
      val ozid = makeOptionZedId()
      scribe.info( s"ozid = ${ozid}" )
//      val z: Id[Zed] = ozid
      "val z: Id.Aux[Zed, UUID] = ozid" should compile
      "Zed( id = makeOptionZedId(), score = 3.14 )" should compile

      val ozidAux = makeOptionZedIdAux()
      scribe.info( s"ozidAux = ${ozidAux}" )
      //      val z: Id[Zed] = ozidAux
      "val z: Id.Aux[Zed, UUID] = ozidAux" should compile
      "Zed( id = makeOptionZedIdAux(), score = 3.14 )" should compile

//      val foo: Identifying.EntityAux[Zed, UUID] = Zed.identifying

//      val ozidEntityAux = makeOptionZedIdEntityAux()
//      scribe.info( s"ozidEntityAux = ${ozidEntityAux}" )
////      val z: Id[Zed] = ozidEntityAux
//      "val z: Id.Aux[Zed, UUID] = ozidEntityAux" should compile
//      "Zed( id = makeOptionZedIdEntityAux(), score = 3.14 )" should compile

      val ozidFullAux = makeOptionZedIdFullAux()
      scribe.info( s"ozidFullAux = ${ozidFullAux}" )
      //      val z: Id[Zed] = ozidFullAux
      "val z: Id.Aux[Zed, UUID] = ozidFullAux" should compile
      "Zed( id = makeOptionZedIdFullAux(), score = 3.14 )" should compile
    }

    "option identifying is derived from underlying type" in {
      val fooIdentifying = Identifying[Foo]
      val oFooIdentifying = implicitly[Identifying[Option[Foo]]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "try identifying is derived from underlying type" in {
      val fooIdentifying = implicitly[Identifying[Foo]]
      val oFooIdentifying = implicitly[Identifying[Try[Foo]]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "ErrorOr identifying is derived from underlying type" in {
      val fooIdentifying = implicitly[Identifying[Foo]]
      val oFooIdentifying = implicitly[Identifying[ErrorOr[Foo]]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "AllErrorsOr identifying is derived from underlying type" in {
      val fooIdentifying = implicitly[Identifying[Foo]]
      val oFooIdentifying = implicitly[Identifying[AllErrorsOr[Foo]]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "AllIssuesOr identifying is derived from underlying type" in {
      val fooIdentifying = implicitly[Identifying[Foo]]
      val oFooIdentifying = implicitly[Identifying[AllIssuesOr[Foo]]]
      val oFooId = oFooIdentifying.next
      oFooId should not be (ShortUUID.zero)

      val fooId = fooIdentifying.next
      fooId should not be (ShortUUID.zero)

      oFooId.getClass shouldBe fooId.getClass
    }

    "create Id of varying types" in {
//      import Foo._

      val suid = ShortUUID()
      val fid: Id[Foo] = Id of suid
//      fid shouldBe a[Id[Foo]]
      fid.toString shouldBe s"FooId(${suid})"
      fid.value shouldBe a[ShortUUID]
      fid.value shouldBe suid

      fid.value shouldBe suid
      suid shouldBe fid.value

      val bid: Id[Bar] = Id of 13L
//      bid shouldBe a[Id[Bar]]
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
//      f shouldBe a[Id[Foo]]
      f.toString shouldBe s"FooId(${fid})"
      f.value shouldBe a[ShortUUID]
      f.value shouldBe fid

      val bid = 17L
      val brep = bid.toString
      val b: Id[Bar] = Id fromString brep
//      b shouldBe a[Id[Bar]]
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
