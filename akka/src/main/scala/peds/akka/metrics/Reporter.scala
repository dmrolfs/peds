package peds.akka.metrics

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import better.files._
import com.codahale.metrics.{ Reporter => CReporter, CsvReporter, MetricFilter, MetricRegistry }
import com.codahale.metrics.graphite.{ PickledGraphite, GraphiteReporter }
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import nl.grons.metrics.scala.InstrumentedBuilder


/**
  * Created by rolfsd on 12/15/15.
  */
object Reporter extends LazyLogging {
  def startReporter( config: Config )( implicit registry: MetricRegistry ): Option[CReporter] = {
    val publishFrequency = if ( config hasPath "publish-frequency" ) {
      config getDuration "publish-frequency"
    } else {
      java.time.Duration.ofSeconds( 10 )
    }

    val reporter = if ( config hasPath "graphite.host" ) {
      val graphiteHost = config getString "graphite.host"
      val graphitePort = if ( config hasPath "graphite.port" ) config getInt "graphite.port" else 2004
      Some( makeGraphiteReporter(graphiteHost, graphitePort) )
    } else if ( config hasPath "csv.dir" ) {
      val path = File( config getString "csv.dir" ).createIfNotExists( asDirectory = true )

      Some(
        CsvReporter
        .forRegistry( registry )
        .convertRatesTo( TimeUnit.SECONDS )
        .convertDurationsTo( TimeUnit.MILLISECONDS )
        .filter( MetricFilter.ALL )
        .build( path.toJava )
      )
    } else {
      None
    }

    reporter foreach { _.start( publishFrequency.toMillis, TimeUnit.MILLISECONDS ) }
    reporter
  }


  def makeGraphiteReporter( host: String, port: Int = 2004 )( implicit registry: MetricRegistry ): GraphiteReporter = {
    val graphite = new PickledGraphite( new InetSocketAddress( host, port ) )
    var result = GraphiteReporter
                 .forRegistry( registry )
                 .convertRatesTo( TimeUnit.SECONDS )
                 .convertDurationsTo( TimeUnit.MILLISECONDS )
                 .filter( MetricFilter.ALL )

    val config = ConfigFactory.load
    val EnvNamePath = "lineup.graphite.env-name"

    result = if ( config hasPath EnvNamePath ) {
      result.prefixedWith( config getString EnvNamePath )
    } else {
      result
    }

    result build graphite
  }
}
