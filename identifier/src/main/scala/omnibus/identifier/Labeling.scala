package omnibus.identifier

import scala.reflect.ClassTag
import scala.util.Try
import omnibus.core._

sealed trait Labeling[E] {
  def label: String
}

trait LowPriorityLabeling {
  implicit def default[T: ClassTag]: Labeling[T] = {
    scribe.debug(
      s"Using DEFAULT labeling for type:[${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}]"
    )
    new MakeLabeling[T]
  }
}

object Labeling extends LowPriorityLabeling {

  def apply[E]( implicit l: Labeling[E] ): Labeling[E] = l

  def empty[T]: Labeling[T] = new EmptyLabel[T]

  def pure[T]( labelFn: => String ): Labeling[T] = {
    new Labeling[T] { override def label: String = labelFn }
  }

  implicit def optionalLabeling[T: ClassTag](
    implicit labeling: Labeling[T]
  ): Labeling[Option[T]] = {
    scribe.debug(
      s"Using Optional labeling for type:[Option[${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}]]"
    )
    pure( labeling.label )
  }

  implicit def tryLabeling[T: ClassTag]( implicit labeling: Labeling[T] ): Labeling[Try[T]] = {
    scribe.debug(
      s"Using Try labeling for type:[Try[${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}]]"
    )
    pure( labeling.label )
  }

  implicit def errorOrLabeling[T: ClassTag](
    implicit labeling: Labeling[T]
  ): Labeling[ErrorOr[T]] = {
    scribe.debug(
      s"Using ErrorOr labeling for type:[ErrorOr[${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}]]"
    )
    pure( labeling.label )
  }

  implicit def allErrorOrLabeling[T: ClassTag](
    implicit labeling: Labeling[T]
  ): Labeling[AllErrorsOr[T]] = {
    scribe.debug(
      s"Using AllErrorsOr labeling for type:[AllErrorsOr[${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}]]"
    )
    pure( labeling.label )
  }

  implicit def allIssuesOrLabeling[T: ClassTag](
    implicit labeling: Labeling[T]
  ): Labeling[AllIssuesOr[T]] = {
    scribe.debug(
      s"Using AllIssuesOr labeling for type:[AllIssuesOr[${implicitly[ClassTag[T]].runtimeClass.safeSimpleName}]]"
    )
    pure( labeling.label )
  }
}

final class MakeLabeling[E: ClassTag] extends Labeling[E] {
  override val label: String = {
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
