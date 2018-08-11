package omnibus.akka.persistence.query

import scala.concurrent.{ ExecutionContext, Future }
import akka.NotUsed
import akka.agent.Agent
import akka.persistence.query.{ EventEnvelope, NoOffset, Offset }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Keep, Sink }

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
  implicit materializer: Materializer,
  ec: ExecutionContext
) {
  import AgentProjection.Directive

  import com.github.ghik.silencer.silent
  @silent val view: Agent[T] = Agent( zero )

  def start(): Future[T] = materializeCurrentView map {
    case ( current, snr ) =>
      scribe.warn( s"#TEST:DEBUG: current materialization: [${current}]" )
      scribe.info( "current view materialized. starting ongoing projection..." )
      startProjectionFrom( snr )
      current
  }

  //todo - revisit - this is probably unnecessary with use of Dynamic stream branching
  private def materializeCurrentView: Future[( T, Long )] = {
    scribe.info( "starting active projection source..." )

    queryJournal
      .currentEventsByTag( tag, offset )
      .via( filterKnownEventsFlow )
      .toMat( sink )( Keep.right )
      .run()
  }

  private def startProjectionFrom( sequenceId: Long ): Unit = {
    scribe.info( "starting active projection" )
    queryJournal
      .eventsByTag( tag, Offset.sequence( sequenceId ) )
      .map { e =>
        scribe.warn( s"TEST AgentProjection processing new tagged event:[${e}]" ); e
      }
      .via( filterKnownEventsFlow )
      .to( sink )
      .run()
  }

  private def filterKnownEventsFlow: Flow[EventEnvelope, Directive[T], NotUsed] = {
    Flow[EventEnvelope]
      .map { e =>
        scribe.warn( s"TEST enter filter - tagged event:[${e}]" ); e
      }
      .collect {
        case EventEnvelope( o, pid, snr, event ) if selectLensFor isDefinedAt event => {
          Directive[T]( selectLensFor( event ), pid, snr, o, event )
        }
      }
  }

  private def sink: Sink[Directive[T], Future[( T, Long )]] = {
    Sink.foldAsync( ( view.get(), 0L ) ) {
      case ( ( _, lastSnr ), d ) =>
        scribe.warn( s"#TEST applying directive:[${d}] @ snr:[${d.sequenceNr}]" )
        view
          .alter { d.lens }
          .map { ( _, math.max( lastSnr, d.sequenceNr ) ) }
    }
  }
}

object AgentProjection {
  case class Directive[T]( lens: T => T, pid: String, sequenceNr: Long, offset: Offset, event: Any )
}
