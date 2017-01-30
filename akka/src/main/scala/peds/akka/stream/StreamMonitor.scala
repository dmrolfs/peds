package peds.akka.stream

import akka.NotUsed
import akka.agent.Agent
import akka.stream.scaladsl.Flow
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.{Logger, StrictLogging}
import nl.grons.metrics.scala.{Meter, MetricName}
import peds.akka.metrics.Instrumented
import peds.commons.util._


/**
  * Created by rolfsd on 12/3/15.
  */
trait StreamMonitor extends Instrumented {
  def label: Symbol
  def meter: Meter
  def count: Long
  def enter: Long
  def exit: Long

  def watch[I, O]( flow: Flow[I, O, NotUsed] ): Flow[I, O, NotUsed] = {
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
  implicit class MakeFlowMonitor[I, O]( val underlying: Flow[I, O, NotUsed] ) extends AnyVal {
    def watchFlow( label: Symbol ): Flow[I, O, NotUsed] = outer.flow( label ).watch( underlying )
    def watchSourced( label: Symbol ): Flow[I, O, NotUsed] = outer.source( label ).watch( underlying )
    def watchConsumed( label: Symbol ): Flow[I, O, NotUsed] = outer.sink( label ).watch( underlying )
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  def add( label: Symbol ): Unit = tracked.alter{ _ :+ label } foreach { t => logger debug csvHeader(t) }
  def set( labels: Symbol* ): Unit = tracked.alter( labels ) foreach { t => logger debug csvHeader(t) }

  def source( label: Symbol ): StreamMonitor = if ( notEnabled ) SilentMonitor else addMonitor( label, SourceMonitor(label) )
  def flow( label: Symbol ): StreamMonitor = if ( notEnabled ) SilentMonitor else addMonitor( label, FlowMonitor(label) )
  def sink( label: Symbol ): StreamMonitor = if ( notEnabled ) SilentMonitor else addMonitor( label, SinkMonitor(label) )

  def addMonitor( label: Symbol, monitor: StreamMonitor ): StreamMonitor = {
    all send { _ + (label -> monitor) }
    monitor
  }

  @inline def publish: Unit = logger debug csvLine( tracked.get(), all.get() )
  private def csvLine( labels: Seq[Symbol], ms: Map[Symbol, StreamMonitor] ): String = {
    labels.map{ l => ms.get(l).map{ m => s"${l.name}:${m.count}"} }.flatten.mkString( ", " )
  }

  @inline def isEnabled: Boolean = logger.underlying.isDebugEnabled
  @inline def notEnabled: Boolean = !isEnabled


  override protected val logger: Logger = Logger( LoggerFactory getLogger "StreamMonitor" )
  private val all: Agent[Map[Symbol, StreamMonitor]] = Agent( Map.empty[Symbol, StreamMonitor] )
  private val tracked: Agent[Seq[Symbol]] = Agent( Seq.empty[Symbol] )

  //todo could incorporate header and line into type class for different formats
  private def csvHeader( labels: Seq[Symbol] ): String = labels map { _.name } mkString ","


  final case class SourceMonitor private[stream]( override val label: Symbol ) extends StreamMonitor {
    override lazy val meter: Meter = metrics.meter( label.name + ".source" )
    override def count: Long = meter.count
    override def enter: Long = count
    override def exit: Long = {
      meter.mark()
      count
    }
    override def toString: String = f"""${label.name}[${count}>]"""
  }

  final case class FlowMonitor private[stream]( override val label: Symbol ) extends StreamMonitor {
    override lazy val meter: Meter = metrics.meter( label.name + ".flow" )
    override def count: Long = meter.count
    override def enter: Long = {
      meter.mark()
      count
    }
    override def exit: Long = {
      meter.mark( -1 )
      count
    }
    override def toString: String = f"""${label.name}[>${count}>]"""
  }

  final case class SinkMonitor private[stream]( override val label: Symbol ) extends StreamMonitor {
    override lazy val meter: Meter = metrics.meter( label.name + ".consumed" )
    override def count: Long = meter.count
    override def enter: Long = {
      meter.mark()
      count
    }
    override def exit: Long = count
    override def toString: String = f"""${label.name}[>${count}]"""
  }

  final case object SilentMonitor extends StreamMonitor {
    override val label: Symbol = 'silent
    override lazy val meter: Meter = metrics.meter( label.name )
    override val count: Long = 0L
    override val enter: Long = 0L
    override val exit: Long = 0L
    override val toString: String = f"""${label.name}[${count}]"""
  }
}
