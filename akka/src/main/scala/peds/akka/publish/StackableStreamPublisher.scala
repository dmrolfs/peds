package peds.akka.publish

import akka.actor.{ Actor, ActorLogging }
import peds.commons.util.Chain._


trait StackableStreamPublisher extends EventPublisher { outer: Actor with ActorLogging =>
  abstract override def publish: peds.akka.publish.Publisher = super.publish +> peds.akka.publish.stream
}
