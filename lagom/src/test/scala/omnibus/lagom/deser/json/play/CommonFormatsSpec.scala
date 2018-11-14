package omnibus.lagom.deser.json.play

import org.scalatest.{ Matchers, WordSpec }
import play.api.libs.json.Json
import CommonFormats._

class CommonFormatsSpec extends WordSpec with Matchers {
  "CommonFormats" should {
    "write Throwable" in {
      val cause = new IllegalArgumentException( "bad argument" )
      val ex = new IllegalStateException( "bad error", cause )

      import java.io._
      val bytes = new ByteArrayOutputStream
      val out = new ObjectOutputStream( bytes )

      out.writeObject( Json.toJson( ex ).toString )
      out.flush()

      val in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray ) )
      val actual = Json.parse( in.readObject().asInstanceOf[String] ).as[Throwable]

      actual.getMessage shouldBe ex.getMessage
      actual.getCause.getClass shouldBe ex.getCause.getClass
      actual.getCause.getMessage shouldBe ex.getCause.getMessage
    }
  }
}
