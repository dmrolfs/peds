package omnibus.akka.metrics

import com.codahale.metrics.MetricRegistry
import nl.grons.metrics.scala.{ HdrMetricBuilder, InstrumentedBuilder, MetricName }

/**
  * Created by rolfsd on 12/15/15.
  */
trait Instrumented extends InstrumentedBuilder {
  override lazy val metricBaseName: MetricName = MetricName( this.getClass )
  override implicit val metricRegistry: MetricRegistry = Instrumented.metricRegistry
  override lazy protected val metricBuilder =
    new HdrMetricBuilder( metricBaseName, metricRegistry, resetAtSnapshot = false )
}

object Instrumented {
  val metricRegistry = new MetricRegistry
}
