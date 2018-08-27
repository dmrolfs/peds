package omnibus.identifier
import omnibus.core.{ AllErrorsOr, AllIssuesOr, ErrorOr }

import scala.reflect.ClassTag
import scala.util.Try

sealed trait Labeling[E] {
  def label: String
}

object Labeling {
  def apply[E]( implicit l: Labeling[E] ): Labeling[E] = l

  implicit def default[T: ClassTag]: Labeling[T] = new MakeLabeling[T]

  implicit def optionalLabeling[T]( implicit labeling: Labeling[T] ): Labeling[Option[T]] = {
    new Labeling[Option[T]] {
      override def label: String = labeling.label
    }
  }

  implicit def tryLabeling[T]( implicit labeling: Labeling[T] ): Labeling[Try[T]] = {
    new Labeling[Try[T]] {
      override def label: String = labeling.label
    }
  }

  implicit def errorOrLabeling[T]( implicit labeling: Labeling[T] ): Labeling[ErrorOr[T]] = {
    new Labeling[ErrorOr[T]] {
      override def label: String = labeling.label
    }
  }

  implicit def allErrorOrLabeling[T]( implicit labeling: Labeling[T] ): Labeling[AllErrorsOr[T]] = {
    new Labeling[AllErrorsOr[T]] {
      override def label: String = labeling.label
    }
  }

  implicit def allIssuesOrLabeling[T](
    implicit labeling: Labeling[T]
  ): Labeling[AllIssuesOr[T]] = {
    new Labeling[AllIssuesOr[T]] {
      override def label: String = labeling.label
    }
  }
}

final class MakeLabeling[E: ClassTag] extends Labeling[E] {
  override val label: String = {
    import omnibus.core._
    implicitly[ClassTag[E]].runtimeClass.safeSimpleName + "Id"
  }
}

abstract class CustomLabeling[E] extends Labeling[E]

final class EmptyLabel[E] extends Labeling[E] {
  override val label: String = ""
}

//  private[identifier] sealed trait LabelDefinitionConflict
//
//  trait MakeLabel { self =>
//    // See eidos.id.Format.UUID for an explanation of this
//    // format: off
//    final def `"You can only define an IdDescriptor in terms of either MakeLabel or CustomLabel"`
//    : LabelDefinitionConflict = null
//
//    implicit final def l(implicit ev: self.type <:< Product): Labeling[this.type] =
//      new Labeling[this.type] {
//        override def label: String = self.productPrefix
//      }
//  }
//
//  trait CustomLabel {
//    final def `"You can only define an IdDescriptor in terms of either MakeLabel or CustomLabel"`
//    : LabelDefinitionConflict = null
//    // format: on
//    def label: String
//
//    private def customLabel: String = label
//
//    implicit final def l: Labeling[this.type] = new Labeling[this.type] {
//      override def label: String = customLabel
//    }
//  }
