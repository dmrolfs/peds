package peds.akka

import akka.actor.{ ActorContext, ActorLogging }
import com.typesafe.scalalogging.LazyLogging
import shapeless.TypeCase
import peds.commons.log.Trace
import peds.commons.util.Chain


package object publish extends LazyLogging {

  private[this] val trace = Trace( "peds.akka.publish", logger )

  /** Publisher is a chained operation that supports publishing via multiple links. If a publishing link returns Left(event), 
   * the next publishing link will be processed; otherwise if Right(event) is returned then publishing will cease.
   */
  type Publisher = Chain.Link[Any, Unit]


  /**
   * EventPublisher specifies 
   */
  trait EventPublisher extends ActorStack with ActorLogging {
    def publish: Publisher = silent
  }


  trait SilentPublisher extends EventPublisher {
    override def publish: Publisher = silent
  }


  /** Publish event to actor's sender */
  def sender( implicit context: ActorContext ): Publisher = ( event: Any ) => trace.block( s"publish.sender($event)" ) {
    context.sender() ! event
    Left( event )
  }

  /** Publish event to ActorSystem's eventStream */
  def stream( implicit context: ActorContext ): Publisher = ( event: Any ) => trace.block( s"publish.stream($event)" ) {
    val target = context.system.eventStream
    val Event = TypeCase[target.Event]
    Event.unapply( event ) foreach { e =>
      logger info s"local stream publishing event:${e} on target:${target}"
      target publish e
    }
    Left( event )
  }

  /** Inert publisher takes no publishing action and continues to next.  */
  val identity: Publisher =  ( event: Any ) => trace.block( s"publish.identity($event)" ) { Left( event ) }

  /** Equivalent to identity publisher; takes no publishing action and continues to next. */
  val silent: Publisher = identity
}
