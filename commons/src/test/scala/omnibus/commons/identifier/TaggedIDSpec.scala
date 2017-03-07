package omnibus.commons.identifier

import org.scalatest._
import com.typesafe.scalalogging.LazyLogging


class TaggedIDSpec extends FlatSpec with Matchers with LazyLogging {

  "A TaggedID" should "match equality if underlying id matches" in {
    val id = ShortUUID()
    TaggedID( 'id, id ) shouldBe TaggedID( 'id, id )
    TaggedID( 'lhs, id ) should not be TaggedID( 'rhs, id )
  }
}