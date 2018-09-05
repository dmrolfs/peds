package omnibus.identifier

import scala.reflect.ClassTag
import scala.language.higherKinds
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

  def empty[T]: Labeling[T] = new EmptyLabeling[T]

  def custom[T]( labelFn: => String ): Labeling[T] = {
    new CustomLabeling[T] { override def label: String = labelFn }
  }

  implicit def wrap[C[_], T: ClassTag: Labeling]: Labeling[C[T]] = custom( Labeling[T].label )
}

final class MakeLabeling[E: ClassTag] extends Labeling[E] {
  override val label: String = implicitly[ClassTag[E]].runtimeClass.safeSimpleName
}

abstract class CustomLabeling[E] extends Labeling[E]

final class EmptyLabeling[E] extends Labeling[E] {
  override val label: String = ""
}
