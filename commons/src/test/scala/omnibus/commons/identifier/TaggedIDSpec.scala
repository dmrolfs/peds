//package omnibus.commons.identifier
//
//import omnibus.identifier.ShortUUID
//import org.scalatest._
//import scribe.Level
//
//class TaggedIDSpec extends FlatSpec with Matchers {
//  scribe.Logger.root
//    .clearHandlers()
//    .clearModifiers()
//    .withHandler( minimumLevel = Some( Level.Trace ) )
//    .replace()
//
//  "A TaggedID" should "match equality if underlying id matches" in {
//    val id = ShortUUID()
//    TaggedID( 'id, id ) shouldBe TaggedID( 'id, id )
//    TaggedID( 'lhs, id ) should not be TaggedID( 'rhs, id )
//  }
//}
