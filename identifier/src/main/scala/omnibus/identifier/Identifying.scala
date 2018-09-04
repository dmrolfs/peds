package omnibus.identifier

import java.util.UUID
import scala.util.Try
import scala.language.{ higherKinds, implicitConversions }
import omnibus.core._

abstract class Identifying[S: Labeling] {
  type Entity
  type ID
  type TID = Id.Aux[Entity, ID]
  protected def tag( value: ID ): TID

//  def label: String

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
    s"""${identifying}(label:"${Labeling[S].label}" id:${idType})"""
  }
}

object Identifying extends LowPriorityCompositeIdentifying {

  type Aux[S, ID0] = Identifying[S] {
    type ID = ID0
  }

  type EntityAux[E, ID0] = Identifying[_] {
    type Entity = E
    type ID = ID0
  }

  type FullAux[S, E, ID0] = Identifying[S] {
    type Entity = E
    type ID = ID0
  }

  def apply[S]( implicit i: Identifying[S] ): FullAux[S, i.Entity, i.ID] = i

  def pure[E: Labeling, I](
    zeroValueFn: => I,
    nextValueFn: () => I,
    valueFromRepFn: String => I
  ): FullAux[E, E, I] = {
    new Simple[E, I]( zeroValueFn, nextValueFn, valueFromRepFn )
  }

  def byShortUuid[E: Labeling]: FullAux[E, E, ShortUUID] = {
    pure[E, ShortUUID](
      zeroValueFn = ShortUUID.zero,
      nextValueFn = () => ShortUUID(),
      valueFromRepFn = rep => {
        import cats.syntax.either._
        ShortUUID.fromString( rep ).valueOr { ex =>
          throw ex
        }
      }
    )
  }

  def byUuid[E: Labeling]: FullAux[E, E, UUID] = {
    val zero = new UUID( 0L, 0L )

    pure[E, UUID](
      zeroValueFn = zero,
      nextValueFn = () => UUID.randomUUID(),
      valueFromRepFn = UUID.fromString
    )
  }

  def byLong[E: Labeling]: FullAux[E, E, Long] = new ByLong[E]

  final class ByLong[E: Labeling] extends Identifying[E] {
    import java.util.concurrent.atomic.AtomicLong
    private[this] val latestId: AtomicLong = new AtomicLong( 0L )

    override type Entity = E
    override type ID = Long
    override protected def tag( value: ID ): TID = Id.of[E, ID]( value )( this, Labeling[E] )
    override val zeroValue: ID = 0L
    override def nextValue: ID = latestId.incrementAndGet()
    override def valueFromRep( rep: String ): ID = rep.toLong
  }

  class Simple[E: Labeling, I](
    zeroValueFn: => I,
    nextValueFn: () => I,
    valueFromRepFn: String => I
  ) extends Identifying[E] {
    override type Entity = E
    override type ID = I
    override protected def tag( value: ID ): TID = Id.of[E, I]( value )( this, Labeling[E] )
    override val zeroValue: ID = zeroValueFn
    override def nextValue: ID = nextValueFn()
    override def valueFromRep( rep: String ): ID = valueFromRepFn( rep )
  }
}

trait LowPriorityCompositeIdentifying {
  self: Identifying.type =>

  def compositePure[C[_], E, I](
    implicit underlying: Aux[E, I],
    cl: Labeling[C[E]],
    l: Labeling[E]
  ): FullAux[C[E], E, I] = {
    new Composite[C, E, I]
  }

  implicit def optionalIdentifying[E, I](
    implicit identifying: Aux[E, I],
    l: Labeling[E]
  ): FullAux[Option[E], E, I] = {
    compositePure[Option, E, I]
  }

  implicit def tryIdentifying[E, I](
    implicit identifying: Aux[E, I],
    l: Labeling[E]
  ): FullAux[Try[E], E, I] = {
    compositePure[Try, E, I]
  }

  implicit def errorOrIdentifying[E, I](
    implicit identifying: Aux[E, I],
    l: Labeling[E]
  ): FullAux[ErrorOr[E], E, I] = {
    compositePure[ErrorOr, E, I]
  }

  implicit def allErrorOrIdentifying[E, I](
    implicit identifying: Aux[E, I],
    l: Labeling[E]
  ): FullAux[AllErrorsOr[E], E, I] = {
    compositePure[AllErrorsOr, E, I]
  }

  implicit def allIssuesOrIdentifying[E, I](
    implicit identifying: Aux[E, I],
    l: Labeling[E]
  ): FullAux[AllIssuesOr[E], E, I] = {
    compositePure[AllIssuesOr, E, I]
  }

  class Composite[C[_], E, I](
    implicit val underlying: Aux[E, I],
    cl: Labeling[C[E]],
    l: Labeling[E]
  ) extends Identifying[C[E]] {
    override type Entity = E
    override type ID = I
    override protected def tag( value: ID ): TID = Id.of[E, I]( value )
    override val zeroValue: ID = underlying.zeroValue
    override def nextValue: ID = underlying.nextValue
    override def valueFromRep( rep: String ): ID = underlying valueFromRep rep
  }
}
