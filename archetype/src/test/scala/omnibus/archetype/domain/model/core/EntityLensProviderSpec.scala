package omnibus.archetype.domain.model.core

import org.scalatest.{ Matchers, WordSpec }
import shapeless.{ lens, Lens }
import omnibus.identifier.{ Identifying, ShortUUID }

object EntityLensProviderSpec {

  case class Bar( override val id: Bar#TID, override val name: String ) extends Entity[Bar, Long]

  object Bar extends EntityLensProvider[Bar, Long] {
    override val identifying = Identifying.byLong

    override val idLens: Lens[Bar, Bar#TID] = lens[Bar].id
    override val nameLens: Lens[Bar, String] = lens[Bar].name
    override val slugLens: Lens[Bar, String] = new Lens[Bar, String] {
      override def get( b: Bar ): String = b.slug
      override def set( b: Bar )( slug: String ): Bar = b
    }
  }

  abstract class Foo extends Entity[Foo, ShortUUID]

  object Foo extends EntityLensProvider[Foo, ShortUUID] {
    def apply( id: Foo#TID, name: String ): Foo = SimpleFoo( id, name )

    override implicit val identifying: Identifying.Aux[Foo, ShortUUID] = Identifying.byShortUuid

    override val idLens: Lens[Foo, Foo#TID] = new Lens[Foo, Foo#TID] {
      override def get( f: Foo ): Foo#TID = f.id
      override def set( f: Foo )( tid: Foo#TID ): Foo = SimpleFoo( id = tid, name = f.name )
    }

    override val nameLens: Lens[Foo, String] = new Lens[Foo, String] {
      override def get( f: Foo ): String = f.name
      override def set( f: Foo )( name: String ): Foo = SimpleFoo( id = f.id, name = name )
    }

    override val slugLens: Lens[Foo, String] = new Lens[Foo, String] {
      override def get( f: Foo ): String = f.slug
      override def set( f: Foo )( slug: String ): Foo = f
    }

    private case class SimpleFoo( override val id: Foo#TID, override val name: String ) extends Foo
  }
}

class EntityLensProviderSpec extends WordSpec with Matchers {
  import EntityLensProviderSpec.{ Bar, Foo }

  "An EntityLensProvider" should {
    "get id from Entity" in {
      val f = Foo( Foo.nextId, "fo" )
      Foo.idLens.get( f ) shouldBe f.id

      val b = Bar( Bar.nextId, "ba" )
      Bar.idLens.get( b ) shouldBe b.id
    }

    "get name from Entity" in {
      val f = Foo( Foo.nextId, "fo" )
      Foo.nameLens.get( f ) shouldBe f.name

      val b = Bar( Bar.nextId, "ba" )
      Bar.nameLens.get( b ) shouldBe b.name
    }

    "get slug from Entity" in {
      val f = Foo( Foo.nextId, "fo" )
      Foo.slugLens.get( f ) shouldBe f.id.value.toString

      val b = Bar( Bar.nextId, "ba" )
      Bar.slugLens.get( b ) shouldBe b.id.value.toString
    }
  }
}
