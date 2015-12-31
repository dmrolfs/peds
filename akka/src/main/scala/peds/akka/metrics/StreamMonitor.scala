package peds.akka.metrics

import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.{ Logger, StrictLogging }
import nl.grons.metrics.scala.{ Counter, MetricName }
import org.slf4j.LoggerFactory
import peds.commons.util._


/**
  * Created by rolfsd on 12/3/15.
  */
trait StreamMonitor extends Instrumented {
  def label: Symbol
  def metric: Counter
  def count: Long
  def enter: Long
  def exit: Long

  def watch[I, O]( flow: Flow[I, O, Unit] ): Flow[I, O, Unit] = {
    Flow[I]
    .map { e =>
      if ( StreamMonitor.isEnabled ) {
        enter
        StreamMonitor.publish
      }

      e
    }
    .via( flow )
    .map { e =>
      if ( StreamMonitor.isEnabled ) {
        exit
        StreamMonitor.publish
      }

      e
    }
  }

  def block[R]( b: () => R ): R = {
    if ( StreamMonitor.notEnabled ) b()
    else {
      enter
      val result = b()
      exit
      result
    }
  }

  override lazy val metricBaseName: MetricName = MetricName( classOf[StreamMonitor].safeSimpleName.toLowerCase )

  override def toString: String = f"""$label[${count}]"""
}

object StreamMonitor extends StrictLogging { outer =>
  def set( labels: Symbol* ): Unit = {
    tracked = labels filter { all contains _ }
    logger info csvHeader( tracked )
  }

  def source( label: Symbol ): StreamMonitor = {
    if ( notEnabled ) SilentMonitor
    else {
      val m = SourceMonitor( label )
      all += ( label -> m )
      m
    }
  }

  def flow( label: Symbol ): StreamMonitor = {
    if ( notEnabled ) SilentMonitor
    else {
      val m = FlowMonitor( label )
      all += ( label -> m )
      m
    }
  }

  def sink( label: Symbol ): StreamMonitor = {
    if ( notEnabled ) SilentMonitor
    else {
      val m = SinkMonitor( label )
      all += ( label -> m )
      m
    }
  }

  @inline def publish: Unit = logger info csvLine( tracked, all )

  @inline def isEnabled: Boolean = logger.underlying.isInfoEnabled
  @inline def notEnabled: Boolean = !isEnabled


  override protected val logger: Logger = Logger( LoggerFactory getLogger "StreamMonitor" )
  private var all: Map[Symbol, StreamMonitor] = Map.empty[Symbol, StreamMonitor]
  private var tracked: Seq[Symbol] = Seq.empty[Symbol]

  //todo could incorporate header and line into type class for different formats
  private def csvHeader( labels: Seq[Symbol] ): String = labels map { _.name } mkString ","
  private def csvLine( labels: Seq[Symbol], ms: Map[Symbol, StreamMonitor] ): String = {
    labels.map{ ms get _ }.flatten.map{_.count }.mkString( "," )
  }


  final case class SourceMonitor private[metrics]( override val label: Symbol ) extends StreamMonitor {
    val Suffix: String = ".sourced.counts"
    override val metric: Counter = metrics.counter( label.name + Suffix )
    override def count: Long = metric.count
    override def enter: Long = count
    override def exit: Long = {
      metric.inc()
      count
    }
    override def toString: String = f"""${label.name}[${count}>]"""
  }

  final case class FlowMonitor private[metrics]( override val label: Symbol ) extends StreamMonitor {
    val Suffix: String = ".in-flow.counts"
    override val metric: Counter = metrics.counter( label.name + Suffix )
    override def count: Long = metric.count
    override def enter: Long = {
      metric.inc()
      count
    }
    override def exit: Long = {
      metric.dec()
      count
    }
    override def toString: String = f"""${label.name}[>${count}>]"""
  }

  final case class SinkMonitor private[metrics]( override val label: Symbol ) extends StreamMonitor {
    val Suffix: String = ".consumed.counts"
    override val metric: Counter = metrics.counter( label.name + Suffix )
    override def count: Long = metric.count
    override def enter: Long = {
      metric.inc()
      count
    }
    override def exit: Long = count
    override def toString: String = f"""${label.name}[>${count}]"""
  }

  final case object SilentMonitor extends StreamMonitor {
    override val label: Symbol = 'silent
    override val metric: Counter = metrics.counter( label.name )
    override val count: Long = 0L
    override val enter: Long = 0L
    override val exit: Long = 0L
    override val toString: String = f"""${label.name}[${count}]"""
  }
}
