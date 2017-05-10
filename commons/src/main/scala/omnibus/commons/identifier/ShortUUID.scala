package omnibus.commons.identifier

import java.nio.ByteBuffer
import cats.syntax.either._
import org.apache.commons.codec.binary.Base64
import com.eaio.uuid.UUID
// import org.hashids._
// import org.hashids.syntax._
import omnibus.commons.ErrorOr


object ShortUUID {
  // implicit val hashids: Hashids = Hashids.reference( "omnibus comprises several items" )

  def apply(): ShortUUID = fromUUID( new UUID )
  def fromString( rep: String ): ShortUUID = new ShortUUID( rep )
  // def fromString( rep: String ): ShortUUID = new ShortUUID( rep.unhashid )

  implicit def fromUUID( uuid: UUID ): ShortUUID = {
    // val clock: Long = uuid.getClockSeqAndNode
    // val isClockNegative: Long = if ( clock < 0L ) 1L else 0L
    // val time: Long = uuid.getTime
    // val isTimeNegative: Long = if ( time < 0L ) 1L else 0L
    // new ShortUUID( List(isClockNegative, math.abs(clock), isTimeNegative, math.abs(time)) )
    val bb = ByteBuffer allocate 16
    bb putLong uuid.getClockSeqAndNode
    bb putLong uuid.getTime
    bb.flip
    val result = new ShortUUID( Base64 encodeBase64URLSafeString bb.array )
    bb.clear
    result
  }

  implicit def toUUID( short: ShortUUID ): UUID = short.toUUID

  val zero: ShortUUID = fromUUID( UUID.nilUUID )


  trait ShortUuidIdentifying[T] { self: Identifying[T] =>
    override type ID = ShortUUID
    override def nextTID: ErrorOr[TID] = tag( ShortUUID() ).asRight
    override def idFromString( idRep: String ): ID = ShortUUID fromString idRep
  }
}

class ShortUUID private[identifier]( val repr: String ) extends Serializable with Equals {
// class ShortUUID( ids: Seq[Long] ) extends Equals {
  // import ShortUUID.hashids

  // lazy val rep: String = ids.hashid

  def isNil: Boolean = this == ShortUUID.zero

  def toUUID: UUID = {
    @inline def actual( magnitude: Long, isNegative: Long ): Long = magnitude * ( if (isNegative == 1L) -1L else 1L )

    // val List( isClockNegative, clockMagnitude, isTimeNegative, timeMagnitude ) = ids
    // val clock = actual( clockMagnitude, isClockNegative )
    // val time = actual( timeMagnitude, isTimeNegative )
    // new UUID( time, clock )
    val bytes = Base64 decodeBase64 repr
    val bb = ByteBuffer allocate 16
    bb put bytes
    bb.flip
    val clock = bb.getLong
    val time = bb.getLong
    new UUID( time, clock )
  }

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[ShortUUID]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: ShortUUID => {
      if ( this eq that ) true
      else {
        // val List( lhsIsClockNegative, lhsClockMagnitude, lhsIsTimeNegative, lhsTimeMagnitude ) = this.ids
        // val List( rhsIsClockNegative, rhsClockMagnitude, rhsIsTimeNegative, rhsTimeMagnitude ) = that.rep.unhashid

        ( that.## == this.## ) &&
        ( that canEqual this ) &&
        ( that.repr == this.repr )
        // ( lhsTimeMagnitude == rhsTimeMagnitude ) &&
        // ( lhsClockMagnitude == rhsClockMagnitude ) &&
        // ( lhsIsClockNegative == rhsIsClockNegative ) &&
        // ( lhsIsTimeNegative == lhsIsTimeNegative )
      }
    }

    case _ => false
  }

  override val hashCode: Int = 41 * ( 41 + repr.## )
  // override val hashCode: Int = {
  //   val List( isClockNegative, clockMagnitude, isTimeNegative, timeMagnitude ) = ids
  //   41 * (
  //     41 * (
  //       41 * (
  //         41 + isClockNegative.##
  //       ) + clockMagnitude.##
  //     ) + isTimeNegative.##
  //   ) + timeMagnitude.##
  // }

  override def toString: String = if ( this == ShortUUID.zero ) "<zero-uuid>" else repr
}
