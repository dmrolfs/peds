package omnibus.akka

import akka.actor.ActorContext
import shapeless.TypeCase
import omnibus.commons.util.Chain


package object publish {
  /** Publisher is a chained operation that supports publishing via multiple links. If a publishing link returns Left(event), 
   * the next publishing link will be processed; otherwise if Right(event) is returned then publishing will cease.
   */
  type Publisher = Chain.Link[Any, Unit]




  /** Publish event to actor's sender */
  def sender( implicit context: ActorContext ): Publisher = ( event: Any ) => {
    context.sender() ! event
    Left( event )
  }

  /** Publish event to ActorSystem's eventStream */
  def stream( implicit context: ActorContext ): Publisher = ( event: Any ) => {
    val target = context.system.eventStream
    val Event = TypeCase[target.Event]
    Event.unapply( event ) foreach { target.publish }
    Left( event )
  }

  /** Inert publisher takes no publishing action and continues to next.  */
  val identity: Publisher =  ( event: Any ) => Left( event )

  /** Equivalent to identity publisher; takes no publishing action and continues to next. */
  val silent: Publisher = identity
}
