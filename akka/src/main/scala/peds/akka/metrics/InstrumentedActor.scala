package peds.akka.metrics

import akka.actor.Actor
import nl.grons.metrics.scala._
import peds.akka.ActorStack


/**
  * Created by rolfsd on 12/15/15.
  */
trait InstrumentedActor
  extends Actor
  with ActorStack
  with Instrumented
  with ReceiveCounterActor
  with ReceiveTimerActor
  with ReceiveExceptionMeterActor


trait ReceiveCounterActor extends Actor with ActorStack { outer: InstrumentedBuilder =>
  def receiveCounterName: String = "receiveCounter"
  lazy val counter: Counter = metrics counter receiveCounterName
  override def around( r: Receive ): Receive = counter.count( super.around(r) )
}


trait ReceiveTimerActor extends Actor with ActorStack { outer: InstrumentedBuilder =>
  def receiveTimerName: String = "receiveTimer"
  lazy val timer: Timer = metrics timer receiveTimerName
  override def around( r: Receive ): Receive = timer.timePF( super.around(r) )
}


trait ReceiveExceptionMeterActor extends Actor with ActorStack { outer: InstrumentedBuilder =>
  def receiveExceptionMeterName: String = "receiveExceptionMeter"
  lazy val meter: Meter = metrics.meter( receiveExceptionMeterName )

  import scala.language.reflectiveCalls
  override def around( r: Receive ): Receive = meter.exceptionMarkerPF( super.around(r) )
}
