package omnibus.commons.identifier

import org.scalatest._

class TaggedIDSpec extends FlatSpec with Matchers {

  "A TaggedID" should "match equality if underlying id matches" in {
    val id = ShortUUID()
    TaggedID( 'id, id ) shouldBe TaggedID( 'id, id )
    TaggedID( 'lhs, id ) should not be TaggedID( 'rhs, id )
  }
}
