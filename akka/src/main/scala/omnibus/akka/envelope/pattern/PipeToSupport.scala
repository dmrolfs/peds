package omnibus.akka.envelope.pattern

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import akka.actor.{ Actor, ActorRef, ActorSelection, Status }
import omnibus.akka.envelope._

/**
  * Created by rolfsd on 6/9/16.
  */
trait PipeToSupport {
  import scala.language.implicitConversions

  /**
    * Import this implicit conversion to gain the `pipeTo` method on [[scala.concurrent.Future]]:
    *
    * {{{
    * import omnibus.akka.envelope.pipe
    *
    * Future { doExpensiveCalc() } pipeEnvelopeTo nextActor
    *
    * or
    *
    * pipeEnvelope(someFuture) to nextActor
    *
    * }}}
    *
    * The successful result of the future is sent as a message to the recipient, or
    * the failure is sent in a [[akka.actor.Status.Failure]] to the recipient.
    */
  implicit def pipe[T](
    future: Future[T]
  )( implicit ec: ExecutionContext ): PipeableFutureEnvelope[T] = {
    new PipeableFutureEnvelope( future )
  }
}

final class PipeableFutureEnvelope[T]( val underlying: Future[T] )(
  implicit ec: ExecutionContext
) {

  def pipeEnvelopeTo(
    recipient: ActorRef
  )( implicit sender: ActorRef = Actor.noSender ): Future[T] = {
    underlying andThen {
      case Success( r ) => recipient !+ r
      case Failure( f ) => recipient !+ Status.Failure( f )
    }
  }

  def to( recipient: ActorRef ): PipeableFutureEnvelope[T] = to( recipient, Actor.noSender )

  def to( recipient: ActorRef, sender: ActorRef ): PipeableFutureEnvelope[T] = {
    pipeEnvelopeTo( recipient )( sender )
    this
  }

  def pipeEnvelopeToSelection(
    recipient: ActorSelection
  )( implicit sender: ActorRef = Actor.noSender ): Future[T] = {
    underlying andThen {
      case Success( r ) => recipient !+ r
      case Failure( f ) => recipient !+ Status.Failure( f )
    }
  }

  def to( recipient: ActorSelection ): PipeableFutureEnvelope[T] = to( recipient, Actor.noSender )

  def to( recipient: ActorSelection, sender: ActorRef ): PipeableFutureEnvelope[T] = {
    pipeEnvelopeToSelection( recipient )( sender )
    this
  }
}
