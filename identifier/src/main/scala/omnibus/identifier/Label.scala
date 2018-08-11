package omnibus.identifier
import scala.reflect.ClassTag

sealed trait Label extends Serializable {
  def label: String
}

object Label {
  def unapply( l: Label ): Option[String] = Option( l.label )
}

final class MakeLabel[D: ClassTag] extends Label {
  type Descriptor = D

  override val label: String = {
    import omnibus.core._
    implicitly[ClassTag[D]].runtimeClass.safeSimpleName + "Id"
  }
}

abstract class CustomLabel extends Label

case object EmptyLabel extends Label {
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
