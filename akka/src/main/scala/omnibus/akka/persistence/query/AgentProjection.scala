package omnibus.akka.persistence.query

import scala.concurrent.{ExecutionContext, Future}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.agent.Agent
import akka.persistence.query.{EventEnvelope2, NoOffset, Offset}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.typesafe.scalalogging.LazyLogging


/**
  * Created by rolfsd on 2/15/17.
  */
class AgentProjection[T](
  queryJournal: QueryJournal.Journal,
  zero: T,
  tag: String,
  selectLensFor: PartialFunction[Any, T => T],
  offset: Offset = NoOffset
)(
  implicit system: ActorSystem,
  materializer: Materializer,
  ec: ExecutionContext
) extends LazyLogging {
  import AgentProjection.Directive

  val view: Agent[T] = Agent( zero )

  def start(): Future[T] = materializeCurrentView map { case (current, snr) =>
    logger.info( "current view materialized. starting ongoing projection..." )
    startProjectionFrom( snr )
    current
  }

  //todo - revisit - this is probably unnecessary with use of Dynamic stream branching
  private def materializeCurrentView: Future[(T, Long)] = {
    logger.info( "starting active projection source..." )

    queryJournal
      .currentEventsByTag( tag, offset )
      .via( filterKnownEventsFlow )
      .toMat( sink )( Keep.right )
      .run()
  }

  private def startProjectionFrom( sequenceId: Long ): Unit = {
    logger.info( "starting active projection" )
    queryJournal
      .eventsByTag( tag, Offset.sequence(sequenceId) )
      .map { e => logger.warn( "TEST AgentProjection processing new tagged event:[{}]", e ); e }
      .via( filterKnownEventsFlow )
      .to( sink )
      .run()
  }

  private def filterKnownEventsFlow: Flow[EventEnvelope2, Directive[T], NotUsed] = {
    Flow[EventEnvelope2]
      .map { e => logger.warn( "TEST enter filter - tagged event:[{}]", e.toString ); e }
      .collect {
        case EventEnvelope2( o, pid, snr, event ) if selectLensFor isDefinedAt event => {
          Directive[T]( selectLensFor(event), pid, snr, o, event )
        }
      }
  }

  private def sink: Sink[Directive[T], Future[(T, Long)]] = {
    Sink.foldAsync( (view.get(), 0L) ) { case ( (_, lastSnr), d ) =>
      logger.warn( "#TEST applying directive:[{}] @ snr:[{}]", d, d.sequenceNr.toString )
      view
        .alter { d.lens }
        .map { ( _, math.max(lastSnr, d.sequenceNr) ) }
    }
  }
}

object AgentProjection {
  case class Directive[T]( lens: T => T, pid: String, sequenceNr: Long, offset: Offset, event: Any )
}
