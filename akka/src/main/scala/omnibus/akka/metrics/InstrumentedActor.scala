package omnibus.akka.metrics

import akka.actor.Actor
import nl.grons.metrics.scala._
import omnibus.akka.ActorStack


/**
  * Created by rolfsd on 12/15/15.
  */
trait InstrumentedActor
extends ActorStack
with Instrumented
with ReceiveCounterStack
with ReceiveTimerStack
with ReceiveExceptionMeterStack { outer: Actor => }


trait ReceiveCounterStack extends ActorStack { outer: Actor with InstrumentedBuilder =>
  def receiveCounterName: String = "receiveCounter"
  lazy val counter: Counter = metrics counter receiveCounterName
  override def around( r: Receive ): Receive = counter.count( super.around(r) )
}


trait ReceiveTimerStack extends ActorStack { outer: Actor with InstrumentedBuilder =>
  def receiveTimerName: String = "receiveTimer"
  lazy val timer: Timer = metrics timer receiveTimerName
  override def around( r: Receive ): Receive = timer.timePF( super.around(r) )
}


trait ReceiveExceptionMeterStack extends ActorStack { outer: Actor with InstrumentedBuilder =>
  def receiveExceptionMeterName: String = "receiveExceptionMeter"
  lazy val meter: Meter = metrics.meter( receiveExceptionMeterName )

  import scala.language.reflectiveCalls
  override def around( r: Receive ): Receive = meter.exceptionMarkerPF( super.around(r) )
}
