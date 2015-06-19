package peds.akka.publish

import peds.commons.log.Trace
import peds.commons.util.Chain._


trait StackableStreamPublisher extends EventPublisher { 
  private val trace = Trace[StackableStreamPublisher]

  abstract override def publish: peds.akka.publish.Publisher = trace.block( "publish" ) { 
    super.publish +> peds.akka.publish.stream 
  }
}
