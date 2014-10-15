package peds.akka

import scala.util.Try
import akka.actor.{ ActorContext, ActorLogging }
import com.typesafe.scalalogging.LazyLogging
import shapeless.syntax.typeable._
import peds.commons.log.Trace
import peds.akka.envelope.Envelope
import peds.commons.util.Chain


package object publish extends LazyLogging {

  private[this] val trace = Trace( "peds.akka.publish", logger )

  type Publisher = Chain.Link[Envelope, Unit]


  trait EventPublisher extends ActorStack with ActorLogging {
    def publish: Publisher = local
  }


  def local( implicit context: ActorContext ): Publisher = ( event: Envelope ) => trace.block( s"publish.local($event)" ) {
    val target = context.system.eventStream
    event.cast[target.Event] foreach { e =>
      logger info s"local stream publishing event:${e} on target:${target}"
      //DMR: somehow need to update envelope per EnvelopeSending.update
      target publish e
    }
    Left( event )
  }

  val silent: Publisher = ( event: Envelope ) => trace.block( s"publish.silent($event)" ) { Left( event ) }
}
