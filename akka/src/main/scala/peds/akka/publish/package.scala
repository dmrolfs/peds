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

  /**
   * Publisher is a chained operation that supports publishing via multiple links. If a publishing link returns Left(event), the
   * next publishing link will be processed; otherwise if Right(event) is returned then publishing will cease.
   */
  type Publisher = Chain.Link[Envelope, Unit]


  /**
   * EventPublisher specifies 
   */
  trait EventPublisher extends ActorStack with ActorLogging {
    def publish: Publisher = stream
  }


  /**
   * Publish event to actor's sender
   */
  def sender( implicit context: ActorContext ): Publisher = ( event: Envelope ) => trace.block( s"publish.sender($event)" ) {
    context.sender() ! event
    Left( event )
  }

  /**
   * Publish event to ActorSystem's eventStream
   */
  def stream( implicit context: ActorContext ): Publisher = ( event: Envelope ) => trace.block( s"publish.stream($event)" ) {
    val target = context.system.eventStream
    event.cast[target.Event] foreach { e =>
      logger info s"local stream publishing event:${e} on target:${target}"
      //DMR: somehow need to update envelope per EnvelopeSending.update
      target publish e
    }
    Left( event )
  }

  /**
   * Throw away published event; i.e., to /dev/null
   */
  val silent: Publisher = ( event: Envelope ) => trace.block( s"publish.silent($event)" ) { Left( event ) }
}
