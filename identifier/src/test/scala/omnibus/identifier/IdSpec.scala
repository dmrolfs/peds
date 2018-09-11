package omnibus.identifier

import org.scalatest.{ Matchers, Tag, WordSpec }
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
    implicit val identifying = Identifying.byShortUuid[Foo]
  }

  case class Bar( id: Id[Bar], b: Double )

  object Bar {
    type TID = identifying.TID
    def nextId: TID = identifying.next
    implicit val identifying = Identifying.byLong[Bar]
  }

  object WIP extends Tag( "wip" )

  "An Id" should {
    "summons Aux" in {
      implicit val fid = Foo.identifying.next
      val Id( shortFid ) = fid
      shortFid shouldBe a[ShortUUID]

      implicit val bid = Bar.nextId
      bid.value.getClass shouldBe classOf[Long] //todo: better handle primitive boxing
    }

    "create Id of varying types" in {
      val suid = ShortUUID()
      val fid: Id[Foo] = Id of suid
      fid shouldBe a[Id[_]]
      fid.toString shouldBe s"FooId(${suid})"
      fid.value shouldBe a[ShortUUID]
      fid.value shouldBe suid

      fid.value shouldBe suid
      suid shouldBe fid.value

      val bid: Id[Bar] = Id of 13L
      bid shouldBe a[Id[_]]
      bid.toString shouldBe "BarId(13)"
      bid.value.getClass shouldBe classOf[java.lang.Long]
      bid.value shouldBe 13L
    }

    "unwrap composites to simple" in {
      val suid = ShortUUID()
      val ofid = Id.of[Option[Foo], ShortUUID]( suid )
      "val id: Id[Foo] = ofid" should compile
      ofid.toString shouldBe s"FooId(${suid})"
      val id: Id[Foo] = ofid
      id shouldBe a[Id[_]]
      id.toString shouldBe s"FooId(${suid})"
      id.value shouldBe a[ShortUUID]
      id.value shouldBe suid
    }

    "invalid id type should fail" in {
      "val fid: Id[Foo] = Id of 17L" shouldNot compile
    }

    "create Id from strings" in {
      val fid = ShortUUID()
      val frep = fid.toString

      val f: Id[Foo] = Id fromString frep
      f shouldBe a[Id[_]]
      f.toString shouldBe s"FooId(${fid})"
      f.value shouldBe a[ShortUUID]
      f.value shouldBe fid

      val bid = 17L
      val brep = bid.toString
      val b: Id[Bar] = Id fromString brep
      b shouldBe a[Id[_]]
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
      implicit val fooLabeling = Labeling.custom[Foo]( "SpecialFoo" )

      val suid = ShortUUID()
      val fid = Id.of[Foo, ShortUUID]( suid )
      fid.toString shouldBe s"SpecialFooId(${suid})"

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

    "be serializable" taggedAs WIP in {
      import java.io._
      val bytes = new ByteArrayOutputStream
      val out = new ObjectOutputStream( bytes )

      val fid = ShortUUID()
      val expected: Id.Aux[Foo, ShortUUID] = Id of fid

      out.writeObject( expected )
      out.flush()

      val in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray ) )
      val actual = in.readObject().asInstanceOf[Id.Aux[Foo, Long]]
      import scala.reflect.ClassTag
      import scala.reflect.runtime.universe._

      val actualClassTag: ClassTag[Id.Aux[Foo, Long]] = ClassTag( actual.getClass )
      scribe.debug( s"actual[${actual}] type = ${actualClassTag}" )

      actual shouldBe expected
      actual.value shouldBe fid
      actual.toString shouldBe s"FooId(${fid})"
    }
  }
}
