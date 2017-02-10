package omnibus.akka.envelope

import omnibus.commons.util._
import com.typesafe.scalalogging.StrictLogging


case class EnvelopeHeader(
  fromComponentType: ComponentType,
  fromComponentPath: ComponentPath,
  toComponentPath: ComponentPath,
  messageType: MessageType,
  workId: WorkId,
  messageNumber: MessageNumber,
  version: EnvelopeVersion,
  properties: Map[Symbol, Any] = Map(),
  createdTimeStamp: Long = System.currentTimeMillis
)


// This is the ultimate class in which we're interested.  It contains all of
// the meta-information we need in order to see what's what
case class Envelope( payload: Any, header: EnvelopeHeader )

object Envelope extends StrictLogging {
  // An implicit conversion makes things easy for us; we can convert from an Any
  // to an Envelope with our implicit
  import scala.language.implicitConversions

  implicit def message2Envelope(
    message: Any
  )(
    implicit fromComponentType: ComponentType = ComponentType.unknown,
    fromComponentPath: ComponentPath = ComponentPath.unknown,
    workId: WorkId = WorkId(), //todo auto create new WorkId if not provided // WorkId.unknown,
    messageNumber: MessageNumber = MessageNumber( -1 ),
    version: EnvelopeVersion = EnvelopeVersion(),
    properties: Map[Symbol, Any] = Map()
  ): Envelope = {
    message match {
      case e: Envelope => e //DMR: update w implicit values?

      case m => {
        Envelope(
          message,
          EnvelopeHeader(
            fromComponentType = fromComponentType,
            fromComponentPath = fromComponentPath,
            toComponentPath = ComponentPath.unknown,
            messageType = MessageType( message.getClass.safeSimpleName ),
            workId = workId,
            messageNumber = messageNumber,
            version = version,
            properties = properties
          )
        )
      }
    }
  }
}
