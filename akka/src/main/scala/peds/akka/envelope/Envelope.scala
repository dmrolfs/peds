package peds.akka.envelope

import akka.actor.ActorRef
import peds.commons._
import peds.commons.util._
import com.typesafe.scalalogging.StrictLogging
import peds.commons.log.Trace


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
  val trace = Trace( "Envelope", logger )
  // An implicit conversion makes things easy for us; we can convert from an Any
  // to an Envelope with our implicit
  import scala.language.implicitConversions

  implicit def message2Envelope(
    message: Any
  )(
    implicit fromComponentType: ComponentType,
    fromComponentPath: ComponentPath,
    workId: WorkId,
    messageNumber: MessageNumber,
    version: EnvelopeVersion,
    properties: Map[Symbol, Any] = Map()
  ): Envelope = trace.block( s"message2Envelope($message)" ) {
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
