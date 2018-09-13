package omnibus.identifier

import omnibus.core.{ AllErrorsOr, AllIssuesOr, ErrorOr }
import org.scalatest.{ Matchers, Tag, WordSpec }
import scribe.Level

import scala.util.Try

class LabelingSpec extends WordSpec with Matchers {
  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler( minimumLevel = Some( Level.Trace ) )
    .replace()

  case class Foo( id: Id[Foo], f: String )

  object Foo

  case class Bar( id: Id[Bar], b: Double )

  object Bar {
    implicit val labeling: Labeling[Bar] = Labeling.custom[Bar]( "SUPER-BAR" )
  }

  case class Zed( id: Id[Zed], z: Long )

  object Zed {
    implicit val labeling: Labeling[Zed] = Labeling.empty
  }

  object WIP extends Tag( "wip" )

  "An Labeling" should {
    "summons" taggedAs WIP in {
      Labeling[Foo] should not be (null)
      Labeling[Option[Foo]] should not be (null)
      Labeling[Try[Bar]] should not be (null)
      Labeling[ErrorOr[Zed]] should not be (null)
      Labeling[AllErrorsOr[Foo]] should not be (null)
      Labeling[AllIssuesOr[Bar]] should not be (null)
    }

    "labeling should work" in {
      Labeling[Foo].label shouldBe "Foo"
      Labeling[Bar].label shouldBe "SUPER-BAR"
      Labeling[Zed].label shouldBe empty
    }

    "option labeling is derived from underlying type" in {
      Labeling[Option[Foo]].label shouldBe "Foo"
      Labeling[Option[Bar]].label shouldBe "SUPER-BAR"
      Labeling[Option[Zed]].label shouldBe empty
    }

    "try labeling is derived from underlying type" in {
      Labeling[Try[Foo]].label shouldBe "Foo"
      Labeling[Try[Bar]].label shouldBe "SUPER-BAR"
      Labeling[Try[Zed]].label shouldBe empty
    }

    "ErrorOr labeling is derived from underlying type" in {
      Labeling[ErrorOr[Foo]].label shouldBe "Foo"
      Labeling[ErrorOr[Bar]].label shouldBe "SUPER-BAR"
      Labeling[ErrorOr[Zed]].label shouldBe empty
    }

    "AllErrorsOr labeling is derived from underlying type" in {
      Labeling[AllErrorsOr[Foo]].label shouldBe "Foo"
      Labeling[AllErrorsOr[Bar]].label shouldBe "SUPER-BAR"
      Labeling[AllErrorsOr[Zed]].label shouldBe empty
    }

    "AllIssuesOr labeling is derived from underlying type" in {
      Labeling[AllIssuesOr[Foo]].label shouldBe "Foo"
      Labeling[AllIssuesOr[Bar]].label shouldBe "SUPER-BAR"
      Labeling[AllIssuesOr[Zed]].label shouldBe empty
    }
  }
}
