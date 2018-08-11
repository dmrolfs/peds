package omnibus.akka.publish

import akka.actor.Actor
import omnibus.akka.ActorStack

/**
  * EventPublisher specifies
  */
trait EventPublisher extends ActorStack { outer: Actor =>
  def publish: Publisher = silent
}

trait SilentPublisher extends EventPublisher { outer: Actor =>
  override def publish: Publisher = silent
}
