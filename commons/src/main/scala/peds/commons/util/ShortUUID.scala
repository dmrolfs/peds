package peds.commons.util

import com.eaio.uuid.UUID
import scala.collection.JavaConversions._
import org.apache.commons.codec.binary.Base64
import java.nio.ByteBuffer


object ShortUUID {
  def apply(): ShortUUID = new UUID

  implicit def uuidToShort( uuid: UUID ): ShortUUID = {
    val bb = ByteBuffer allocate 16
    bb putLong uuid.getClockSeqAndNode
    bb putLong uuid.getTime
    bb.flip
    val result = ShortUUID( Base64 encodeBase64URLSafeString bb.array )
    bb.clear
    result
  }

  implicit def short2UUID( short: ShortUUID ): UUID = short.toUUID

  val nilShortUUID: ShortUUID = UUID.nilUUID
}

case class ShortUUID( value: String ) {
  def this() = this( ShortUUID.uuidToShort( new UUID ).toString )

  def isNil: Boolean = this == ShortUUID.nilShortUUID

  def toUUID: UUID = {
    val bytes = Base64 decodeBase64 value
    val bb = ByteBuffer allocate 16
    bb put bytes
    bb.flip
    val clock = bb.getLong
    val time = bb.getLong
    new UUID( time, clock )
  }

  override def toString: String = value
}
