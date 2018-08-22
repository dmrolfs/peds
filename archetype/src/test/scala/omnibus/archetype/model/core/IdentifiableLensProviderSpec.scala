package omnibus.archetype.model.core
import omnibus.archetype.domain.model.core.{
  Identifiable,
  IdentifiableLensProvider,
  IdentifiableLike
}
import omnibus.identifier
import omnibus.identifier.Identifying
import org.scalatest.{ Matchers, WordSpec }
import scribe.Level
import shapeless.{ lens, Lens }

class IdentifiableLensProviderSpec extends WordSpec with Matchers {
  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  case class Bar( override val id: Bar#TID, name: String ) extends Identifiable[Bar]

  object Bar extends IdentifiableLensProvider[Bar] {
    override val identifying: Identifying[Bar] = new identifier.Identifying.ByLong[Bar]
    def idLens: Lens[Bar, Bar#TID] = lens[Bar].id
  }

  abstract class Foo extends IdentifiableLike {
    override type E = Foo
    def name: String
  }

  object Foo extends IdentifiableLensProvider[Foo] {
    def apply( id: Foo#TID, name: String ): Foo = SimpleFoo( id, name )

    implicit val identifying = new Identifying.ByShortUuid[Foo]

    override def idLens: Lens[Foo, Foo#TID] = new Lens[Foo, Foo#TID] {
      override def get( f: Foo ): Foo#TID = f.id
      override def set( f: Foo )( tid: Foo#TID ): Foo = SimpleFoo( id = tid, name = f.name )
    }

    case class SimpleFoo( override val id: Foo#TID, override val name: String ) extends Foo {
      override type E = Foo
    }
  }

  "An IdentifiableLensProvider" should {
    "get id from Entity" in {
      val f = Foo( Foo.identifying.next, "damon" )
      Foo.idLens.get( f ) shouldBe f.id

      val b = Bar( Bar.identifying.next, "rolfs" )
      Bar.idLens.get( b ) shouldBe b.id
    }
  }
}
