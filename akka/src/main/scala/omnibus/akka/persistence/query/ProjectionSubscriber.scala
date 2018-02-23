package omnibus.akka.persistence.query

import akka.NotUsed
import akka.persistence.query.{EventEnvelope, NoOffset, Offset}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import journal._


/**
  * Created by rolfsd on 2/16/17.
  */
class ProjectionSubscriber(
  queryJournal: QueryJournal.Journal,
  tag: String,
  applyLensFor: PartialFunction[Any, Unit],
  offset: Offset = NoOffset
)(
  implicit materializer: Materializer
) {
  private val logger = Logger[ProjectionSubscriber]

  def run(): NotUsed = {
    logger.info( "starting active projection subscriber...")
    queryJournal
    .eventsByTag( tag, offset )
    .map { e => logger.warn( s"#TEST processing new tagged event: [${e}]" ); e }
    //      .via( filterKnownEventsFlow )
    .to(
      Sink foreach { envelope: EventEnvelope =>
        logger.warn( s"#TEST applying lens @ snr:[${envelope.sequenceNr}] to event-envelope:[${envelope}]" )
        applyLensFor( envelope.event )
      }
    )
    .run()
  }

//  private def filterKnownEventsFlow: Flow[EventEnvelope, Any, NotUsed] = {
//    Flow[EventEnvelope]
//      .map { e => logger.warn( "#TEST enter filter - tagged event:[{}]", e.toString ); e }
//      .collect {
//        case EventEnvelope( o, pid, snr, event ) if applyLensFor isDefinedAt event =>
//      }
//  }

//  private def sink: Sink[EventEnvelope, Future[Done]] = {
//    Sink.foreach { envelope: EventEnvelope =>
//      logger.warn( s"#TEST applying lens @ snr:[${envelope.sequenceNr}] to event-envelope:[${envelope}]" )
//      applyLensFor( envelope.event )
//    }
//  }
}
