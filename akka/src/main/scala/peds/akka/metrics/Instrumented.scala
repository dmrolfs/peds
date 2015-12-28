package peds.akka.metrics

import com.codahale.metrics.MetricRegistry
import nl.grons.metrics.scala.InstrumentedBuilder


/**
  * Created by rolfsd on 12/15/15.
  */
trait Instrumented extends InstrumentedBuilder {
  override implicit val metricRegistry: MetricRegistry = Instrumented.metricRegistry
}

object Instrumented {
  val metricRegistry = new MetricRegistry
}
