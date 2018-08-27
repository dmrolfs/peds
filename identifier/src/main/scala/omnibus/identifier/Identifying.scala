package omnibus.identifier

import java.util.UUID

import omnibus.core._

import scala.util.Try

abstract class Identifying[E: Labeling] {
  type ID
  type TID = Id.Aux[E, ID]

  def label: String = Labeling[E].label

  implicit private val self = this.asInstanceOf[Identifying.Aux[E, ID]]

  final def zero: TID = Id.of( zeroValue )
  final def next: TID = Id.of( nextValue )
  final def fromString( rep: String ): TID = Id.of( valueFromRep( rep ) )
  final def of( value: ID ): TID = Id.of( value )

  def zeroValue: ID
  def nextValue: ID
  def valueFromRep( rep: String ): ID

  override def toString: String = {
    val identifying = this.getClass.safeSimpleName
    val idType = zeroValue.getClass.safeSimpleName
    s"""${identifying}(label:"${label}" id:${idType})"""
  }
}

object Identifying {

  type Aux[E, ID0] = Identifying[E] {
    type ID = ID0
  }

  def apply[E]( implicit i: Identifying[E] ): Aux[E, i.ID] = i

  def pure[E: Labeling, I](
    zeroValueFn: => I,
    nextValueFn: => I,
    valueFromRepFn: String => I
  ): Aux[E, I] = {
    new Identifying[E] {
      override type ID = I
      override def zeroValue: ID = zeroValueFn
      override def nextValue: ID = nextValueFn
      override def valueFromRep( rep: String ): ID = valueFromRepFn( rep )
    }
  }

  def byShortUuid[E: Labeling]: Aux[E, ShortUUID] = {
    pure(
      zeroValueFn = ShortUUID.zero,
      nextValueFn = ShortUUID(),
      valueFromRepFn = rep => {
        import cats.syntax.either._
        ShortUUID.fromString( rep ).valueOr { ex =>
          throw ex
        }
      }
    )
  }

  final class ByShortUuid[E: Labeling] extends Identifying[E] {
    override type ID = ShortUUID
    override val zeroValue: ID = ShortUUID.zero
    override def nextValue: ID = ShortUUID()
    override def valueFromRep( rep: String ): ID = {
      import cats.syntax.either._
      ShortUUID.fromString( rep ).valueOr { ex =>
        throw ex
      }
    }
  }

  def byUuid[E: Labeling]: Aux[E, UUID] = {
    val zero = new UUID( 0L, 0L )

    pure(
      zeroValueFn = zero,
      nextValueFn = UUID.randomUUID(),
      valueFromRepFn = UUID.fromString
    )
  }

  final class ByUuid[E: Labeling] extends Identifying[E] {
    override type ID = UUID
    override val zeroValue: ID = new UUID( 0L, 0L )
    override def nextValue: ID = UUID.randomUUID()
    override def valueFromRep( rep: String ): ID = UUID fromString rep
  }

  def byLong[E: Labeling]: Aux[E, Long] = new ByLong[E]

  final class ByLong[E: Labeling] extends Identifying[E] {
    import java.util.concurrent.atomic.AtomicLong
    private[this] val latestId: AtomicLong = new AtomicLong( 0L )

    override type ID = Long
    override val zeroValue: ID = 0L
    override def nextValue: ID = latestId.incrementAndGet()
    override def valueFromRep( rep: String ): ID = rep.toLong
  }

  import scala.language.implicitConversions

  implicit def optionalIdentifying[E, I](
    implicit identifying: Aux[E, I]
  ): Aux[Option[E], I] = {
    new Identifying[Option[E]] {
      override type ID = identifying.ID
      override def label: String = identifying.label
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def tryIdentifying[E, I](
    implicit identifying: Aux[E, I]
  ): Aux[Try[E], I] = {
    new Identifying[Try[E]] {
      override type ID = identifying.ID
      override def label: String = identifying.label
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def errorOrIdentifying[E, I](
    implicit identifying: Aux[E, I]
  ): Aux[ErrorOr[E], I] = {
    new Identifying[ErrorOr[E]] {
      override type ID = identifying.ID
      override def label: String = identifying.label
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def allErrorOrIdentifying[E, I](
    implicit identifying: Aux[E, I]
  ): Aux[AllErrorsOr[E], I] = {
    new Identifying[AllErrorsOr[E]] {
      override type ID = identifying.ID
      override def label: String = identifying.label
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def allIssuesOrIdentifying[E, I](
    implicit identifying: Aux[E, I]
  ): Aux[AllIssuesOr[E], I] = {
    new Identifying[AllIssuesOr[E]] {
      override type ID = identifying.ID
      override def label: String = identifying.label
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }
}
