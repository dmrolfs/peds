package omnibus.akka.persistence.query

import scala.concurrent.{ExecutionContext, Future}
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.persistence.query.{EventEnvelope, NoOffset, Offset}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging


/**
  * Created by rolfsd on 2/16/17.
  */
class ProjectionSubscriber(
  queryJournal: QueryJournal.Journal,
  tag: String,
  applyLensFor: PartialFunction[Any, Unit],
  offset: Offset = NoOffset
)(
  implicit system: ActorSystem,
  materializer: Materializer//,
//  ec: ExecutionContext
) extends LazyLogging {
  def run(): NotUsed = {
    logger.info( "starting active projection subscriber...")
    queryJournal
    .eventsByTag( tag, offset )
    .map { e => logger.warn( "#TEST processing new tagged event: [{}]", e ); e }
    //      .via( filterKnownEventsFlow )
    .to(
      Sink foreach { envelope: EventEnvelope =>
        logger.warn( "#TEST applying lens @ snr:[{}] to event-envelope:[{}]", envelope.sequenceNr.toString, envelope.toString )
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

  private def sink: Sink[EventEnvelope, Future[Done]] = {
    Sink.foreach { envelope: EventEnvelope =>
      logger.warn( "#TEST applying lens @ snr:[{}] to event-envelope:[{}]", envelope.sequenceNr.toString, envelope.toString )
      applyLensFor( envelope.event )
    }
  }
}
