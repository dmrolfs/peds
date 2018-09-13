package omnibus.identifier

import java.util.UUID
import scala.language.{ higherKinds, implicitConversions }
import omnibus.core._

abstract class Identifying[E] extends Serializable {
  type ID
  type TID = Id.Aux[E, ID]

  def as[T: Labeling]: Identifying.Aux[T, ID] = {
    Identifying.pure[T, ID](
      zeroValueFn = this.zeroValue,
      nextValueFn = () => this.nextValue,
      valueFromRepFn = this.valueFromRep( _: String )
    )
  }

  protected def tag( value: ID ): TID

  def label: String

  final def zero: TID = tag( zeroValue )
  final def next: TID = tag( nextValue )
  final def fromString( rep: String ): TID = tag( valueFromRep( rep ) )
  final def of( value: ID ): TID = tag( value )

  def zeroValue: ID
  def nextValue: ID
  def valueFromRep( rep: String ): ID

  override def toString: String = {
    val identifying = this.getClass.safeSimpleName
    val idType = zeroValue.getClass.safeSimpleName
    s"""${identifying}(label:"${label}" id:${idType})"""
  }
}

sealed trait IdentifyingCompanion {

  type Aux[S, ID0] = Identifying[S] {
    type ID = ID0
  }
}

object Identifying extends IdentifyingCompanion with LowPriorityIdentifying {

  def apply[E]( implicit i: Identifying[E] ): Aux[E, i.ID] = i

  def pure[E: Labeling, I](
    zeroValueFn: => I,
    nextValueFn: () => I,
    valueFromRepFn: String => I
  ): Aux[E, I] = {
    Simple[E, I]( zeroValueFn, nextValueFn, valueFromRepFn )
  }

  def byShortUuid[E: Labeling]: Aux[E, ShortUUID] = {
    pure[E, ShortUUID](
      zeroValueFn = ShortUUID.zero,
      nextValueFn = () => ShortUUID(),
      valueFromRepFn = rep => {
        import cats.syntax.either._
        ShortUUID.fromString( rep ) valueOr { ex =>
          throw ex
        }
      }
    )
  }

  def byUuid[E: Labeling]: Aux[E, UUID] = {
    val zero = new UUID( 0L, 0L )

    pure[E, UUID](
      zeroValueFn = zero,
      nextValueFn = () => UUID.randomUUID(),
      valueFromRepFn = UUID.fromString
    )
  }

  def byLong[E: Labeling]: Aux[E, Long] = new ByLong[E]

  final class ByLong[E: Labeling] extends Identifying[E] {
    import java.util.concurrent.atomic.AtomicLong
    private[this] val latestId: AtomicLong = new AtomicLong( 0L )

    override type ID = Long
    override protected def tag( value: ID ): TID = Id.of[E, ID]( value )( this, Labeling[E] )
    override val label: String = Labeling[E].label
    override val zeroValue: ID = 0L
    override def nextValue: ID = latestId.incrementAndGet()
    override def valueFromRep( rep: String ): ID = rep.toLong
  }

  private case class Simple[E: Labeling, I](
    override val zeroValue: I,
    nextValueFn: () => I,
    valueFromRepFn: String => I
  ) extends Identifying[E] {
    override type ID = I
    override protected def tag( value: ID ): TID = Id.of[E, I]( value )( this, Labeling[E] )
    override val label: String = Labeling[E].label
    override def nextValue: ID = nextValueFn()
    override def valueFromRep( rep: String ): ID = valueFromRepFn( rep )
  }
}

trait LowPriorityIdentifying { self: IdentifyingCompanion =>
  implicit def wrap[C[_], E, I](
    implicit underlying: Aux[E, I],
    lce: Labeling[C[E]]
  ): Aux[C[E], I] = {
    HigherKinded[C, E, I]( underlying )
  }

  private case class HigherKinded[C[_], E, I](
    val underlying: Aux[E, I]
  )(
    implicit lce: Labeling[C[E]]
  ) extends Identifying[C[E]] {
    override type ID = I
    override protected def tag( value: ID ): TID = Id.of[C[E], I]( value )( this, Labeling[C[E]] )
    override val label: String = underlying.label
    override val zeroValue: ID = underlying.zeroValue
    override def nextValue: ID = underlying.nextValue
    override def valueFromRep( rep: String ): ID = underlying valueFromRep rep
  }

}
