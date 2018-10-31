package omnibus.identifier

import io.circe.Json.JString
import org.scalatest.{ EitherValues, Matchers, Tag, WordSpec }
import play.api.libs.json.{ Json => PJson, _ }
import io.circe.{ Json => CJson, _ }
import io.circe.parser._
import io.circe.syntax._
import journal._

import scala.util.Random

class IdSpec extends WordSpec with Matchers with EitherValues {
  private val log = Logger[IdSpec]

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

  case class Zoo( id: Zoo.TID, animal: String )

  object Zoo {
    type TID = identifying.TID
    def nextId: TID = identifying.next
    implicit val identifying = Identifying.bySnowflake[Zoo]()
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

      val zuid = Zoo.nextId.value
      val zid: Id[Zoo] = Id of zuid
      zid shouldBe a[Id[_]]
      zid.toString shouldBe s"ZooId(${zuid})"
      zid.value shouldBe a[String]
      zid.value shouldBe zuid

      zid.value shouldBe zuid
      zuid shouldBe zid.value
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

      val zid = Zoo.nextId.value
      val zrep = zid.toString

      val z: Id[Zoo] = Id fromString zrep
      z shouldBe a[Id[_]]
      z.toString shouldBe s"ZooId(${zid})"
      z.value shouldBe a[String]
      z.value shouldBe zid
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

    "support conversion to another entity basis" in {
      val fid: Id.Aux[Foo, ShortUUID] = Foo.nextId
      "val bid = fid.as[Bar]" shouldNot compile
      implicit val barShortIdentifying = Foo.identifying.as[Bar]
      "val bid = fid.as[Bar]" should compile
      val bid: Id.Aux[Bar, ShortUUID] = fid.as[Bar]
      bid.value shouldBe fid.value
      bid.toString shouldBe s"BarId(${fid.value})"
    }

    "be serializable" in {
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
      log.debug( s"actual[${actual}] type = ${actualClassTag}" )

      actual shouldBe expected
      actual.value shouldBe fid
      actual.toString shouldBe s"FooId(${fid})"
    }

    "pretty id is serializable" in {
      import java.io._
      val bytes = new ByteArrayOutputStream
      val out = new ObjectOutputStream( bytes )

      val zid = Zoo.nextId.value
      val expected: Id.Aux[Zoo, String] = Id of zid

      out.writeObject( expected )
      out.flush()

      val in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray ) )
      val actual = in.readObject().asInstanceOf[Id.Aux[Zoo, String]]
      import scala.reflect.ClassTag
      import scala.reflect.runtime.universe._

      val actualClassTag: ClassTag[Id.Aux[Zoo, String]] = ClassTag( actual.getClass )
      log.debug( s"actual[${actual}] type = ${actualClassTag}" )

      actual shouldBe expected
      actual.value shouldBe zid
      actual.toString shouldBe s"ZooId(${zid})"
    }

    "encode to Circe Json" taggedAs WIP in {
      val fid: Id.Aux[Foo, ShortUUID] = Foo.nextId
      val bid: Id.Aux[Bar, Long] = Bar.nextId
      val zid: Id.Aux[Zoo, String] = Zoo.nextId

      fid.asJson shouldBe CJson.fromString( fid.value.toString )
      bid.asJson shouldBe CJson.fromString( bid.value.toString )
      zid.asJson shouldBe CJson.fromString( zid.value.toString )

      val fid2: Id[Foo] = fid
      fid2.asJson shouldBe CJson.fromString( fid.value.toString )

      val bid2: Id[Bar] = bid
      bid2.asJson shouldBe CJson.fromString( bid.value.toString )

      val zid2: Id[Zoo] = zid
      zid2.asJson shouldBe CJson.fromString( zid.value.toString )
    }

    "decode from Circe Json" taggedAs WIP in {
      val sidValue = ShortUUID()
      val lidValue = Random.nextLong()
      val zidValue = Zoo.nextId.value

      val fidJson = CJson fromString sidValue.toString
      val lidJson = CJson fromLong lidValue
      val zidJson = CJson fromString zidValue

      parser.parse( fidJson.noSpaces ).flatMap( _.as[ShortUUID] ).right.get shouldBe sidValue
      parser.parse( lidJson.noSpaces ).flatMap( _.as[Long] ).right.get shouldBe lidValue
      parser.parse( zidJson.noSpaces ).flatMap( _.as[String] ).right.get shouldBe zidValue
    }

    "write to PlayJson" taggedAs WIP in {
      val fid: Id.Aux[Foo, ShortUUID] = Foo.nextId
      val bid: Id.Aux[Bar, Long] = Bar.nextId
      val zid: Id.Aux[Zoo, String] = Zoo.nextId

      PJson.toJson( fid ) shouldBe JsString( fid.value.toString )
      PJson.toJson( bid ) shouldBe JsString( bid.value.toString )
      PJson.toJson( zid ) shouldBe JsString( zid.value.toString )

      val fid2: Id[Foo] = fid
      PJson.toJson( fid2 ) shouldBe JsString( fid.value.toString )

      val bid2: Id[Bar] = bid
      PJson.toJson( bid2 ) shouldBe JsString( bid.value.toString )

      val zid2: Id[Zoo] = zid
      PJson.toJson( zid2 ) shouldBe JsString( zid.value.toString )
    }

    "read from play Json" taggedAs WIP in {
      val sidValue = ShortUUID()
      val lidValue = Random.nextLong()
      val zidValue = Zoo.nextId.value

      val fidJson = PJson toJson sidValue.toString
      val lidJson = PJson toJson lidValue
      val zidJson = PJson toJson zidValue

      parser.parse( fidJson.toString ).flatMap( _.as[ShortUUID] ).right.get shouldBe sidValue
      parser.parse( lidJson.toString ).flatMap( _.as[Long] ).right.get shouldBe lidValue
      parser.parse( zidJson.toString ).flatMap( _.as[String] ).right.get shouldBe zidValue
    }
  }
}
