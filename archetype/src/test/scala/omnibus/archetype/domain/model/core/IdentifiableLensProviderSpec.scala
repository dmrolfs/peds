package omnibus.archetype.domain.model.core

import org.scalatest.{ Matchers, WordSpec }
import shapeless.{ lens, Lens }
import io.jvm.uuid.UUID
import omnibus.identifier.{ Identifying, ShortUUID }

object IdentifiableLensProviderSpec {

  case class Bar( override val id: Bar#TID, name: String ) extends Identifiable[Bar, Long]

  object Bar extends IdentifiableLensProvider[Bar, Long] {
    override implicit val identifying: Identifying.Aux[Bar, Long] = Identifying.byLong

    val idLens: Lens[Bar, Bar#TID] = lens[Bar].id
  }

  abstract class Foo extends Identifiable[Foo, ShortUUID] {
    def name: String
  }

  object Foo extends IdentifiableLensProvider[Foo, ShortUUID] {
    def apply( id: Foo#TID, name: String ): Foo = SimpleFoo( id, name )

    override implicit val identifying: Identifying.Aux[Foo, ShortUUID] = Identifying.byShortUuid

    override val idLens: Lens[Foo, Foo#TID] = new Lens[Foo, Foo#TID] {
      override def get( f: Foo ): Foo#TID = f.id
      override def set( f: Foo )( tid: Foo#TID ): Foo = SimpleFoo( id = tid, name = f.name )
    }

    private case class SimpleFoo( override val id: Foo#TID, override val name: String ) extends Foo
  }

  case class Zed( override val id: Zed#TID, age: Int ) extends Identifiable[Zed, UUID]

  object Zed {
    implicit val identifying: Identifying.Aux[Zed, UUID] = Identifying.byUuid

    val idLens: Lens[Zed, Zed#TID] = lens[Zed].id
  }
}

class IdentifiableLensProviderSpec extends WordSpec with Matchers {
  import IdentifiableLensProviderSpec.{ Bar, Foo, Zed }

  "An IdentifiableLensProvider" should {
    "get id from Entity" in {
      val f = Foo( Foo.nextId, "fo" )
      Foo.idLens.get( f ) shouldBe f.id

      val b = Bar( Bar.nextId, "ba" )
      Bar.idLens.get( b ) shouldBe b.id

      val z = Zed( Zed.identifying.next, 48 )
      Zed.idLens.get( z ) shouldBe z.id
    }
  }
}
