package peds.akka

import peds.akka.envelope.Envelope


package object publish {

  sealed trait ReliableProtocol
  case class ReliableMessage( deliveryId: Long, message: Envelope ) extends ReliableProtocol
  case class Confirm( deliveryId: Long ) extends ReliableProtocol


  case class RedeliveryFailedException( message: Any ) extends RuntimeException
}
