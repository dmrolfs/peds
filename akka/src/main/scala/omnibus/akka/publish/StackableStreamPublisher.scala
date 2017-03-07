package omnibus.akka.publish

import akka.actor.{ Actor, ActorLogging }
import omnibus.commons.util.Chain._


trait StackableStreamPublisher extends EventPublisher { outer: Actor with ActorLogging =>
  abstract override def publish: omnibus.akka.publish.Publisher = super.publish +> omnibus.akka.publish.stream
}
