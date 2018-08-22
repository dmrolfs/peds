package omnibus.identifier

import java.util.UUID

import omnibus.core._

import scala.util.Try

abstract class Identifying[E: Labeling] {
  type ID
  type TID = Id[E]

  def label: String = implicitly[Labeling[E]].label

  implicit protected val self: Identifying.Aux[E, ID] = this.asInstanceOf[Identifying.Aux[E, ID]]

  final def zero: TID = Id of zeroValue
  final def next: TID = Id of nextValue
  final def fromString( rep: String ): TID = Id of valueFromRep( rep )
  final def of( value: ID ): TID = Id of value

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
  type Aux[E, I] = Identifying[E] { type ID = I }

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

  final class ByUuid[E: Labeling] extends Identifying[E] {
    override type ID = UUID
    override val zeroValue: ID = new UUID( 0L, 0L )
    override def nextValue: ID = UUID.randomUUID()
    override def valueFromRep( rep: String ): ID = UUID fromString rep
  }

  final class ByLong[E: Labeling] extends Identifying[E] {
    import java.util.concurrent.atomic.AtomicLong
    private[this] val latestId: AtomicLong = new AtomicLong( 0L )

    override type ID = Long
    override val zeroValue: ID = 0L
    override def nextValue: ID = latestId.incrementAndGet()
    override def valueFromRep( rep: String ): ID = rep.toLong
  }

  import scala.language.implicitConversions

  implicit def optionalIdentifying[E: Identifying]: Identifying[Option[E]] = {
    val identifying = implicitly[Identifying[E]]

    new Identifying[Option[E]] {
      override type ID = identifying.ID
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def tryIdentifying[E: Identifying]: Identifying[Try[E]] = {
    val identifying = implicitly[Identifying[E]]

    new Identifying[Try[E]] {
      override type ID = identifying.ID
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def errorOrIdentifying[E: Identifying]: Identifying[ErrorOr[E]] = {
    val identifying = implicitly[Identifying[E]]

    new Identifying[ErrorOr[E]] {
      override type ID = identifying.ID
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def allErrorOrIdentifying[E: Identifying]: Identifying[AllErrorsOr[E]] = {
    val identifying = implicitly[Identifying[E]]

    new Identifying[AllErrorsOr[E]] {
      override type ID = identifying.ID
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }

  implicit def allIssuesOrIdentifying[E: Identifying]: Identifying[AllIssuesOr[E]] = {
    val identifying = implicitly[Identifying[E]]

    new Identifying[AllIssuesOr[E]] {
      override type ID = identifying.ID
      override def zeroValue: ID = identifying.zeroValue
      override def nextValue: ID = identifying.nextValue
      override def valueFromRep( rep: String ): ID = identifying valueFromRep rep
    }
  }
}
