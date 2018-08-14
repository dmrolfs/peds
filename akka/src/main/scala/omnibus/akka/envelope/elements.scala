package omnibus.akka.envelope

case class RequestId( trackingId: WorkId, spanId: MessageNumber )

object RequestId {

  def summon()( implicit workId: WorkId, messageNumber: MessageNumber ): RequestId = {
    RequestId( trackingId = workId, spanId = messageNumber )
  }
}
