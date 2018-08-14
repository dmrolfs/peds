package omnibus.akka.envelope

import omnibus.core._

case class EnvelopeHeader(
  fromComponentType: ComponentType,
  fromComponentPath: ComponentPath,
  toComponentPath: ComponentPath,
  messageType: MessageType,
  workId: WorkId,
  messageNumber: MessageNumber,
  version: EnvelopeVersion,
  properties: EnvelopeProperties,
  createdTimeStamp: Long = System.currentTimeMillis
)

// This is the ultimate class in which we're interested.  It contains all of
// the meta-information we need in order to see what's what
case class Envelope( payload: Any, header: EnvelopeHeader )

object Envelope {
  // An implicit conversion makes things easy for us; we can convert from an Any
  // to an Envelope with our implicit
  import scala.language.implicitConversions

  implicit def message2Envelope(
    message: Any
  )(
    implicit version: EnvelopeVersion = currentEnvelopeVersion,
    fromComponentType: ComponentType = ComponentType.noSender,
    fromComponentPath: ComponentPath = ComponentPath.noSender,
    workId: WorkId = WorkId(),
    messageNumber: MessageNumber = MessageNumber( -1 ),
    properties: EnvelopeProperties = emptyEnvelopeProperties
  ): Envelope = {
    message match {
      case e: Envelope => e //DMR: update w implicit values?

      case _ => {
        Envelope(
          message,
          EnvelopeHeader(
            fromComponentType = fromComponentType,
            fromComponentPath = fromComponentPath,
            toComponentPath = ComponentPath.noSender,
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
