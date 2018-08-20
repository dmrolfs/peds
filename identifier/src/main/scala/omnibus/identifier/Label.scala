package omnibus.identifier
import scala.reflect.ClassTag

sealed trait Labeling[E] {
  def label: String
}

object Labeling {
  implicit def default[T: ClassTag]: Labeling[T] = new MakeLabeling[T]
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
