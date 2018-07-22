package omnibus.commons

import scala.concurrent.{ ExecutionContext, Future }
import scala.util._
import monix.eval.Task
import monix.execution.Scheduler

package object concurrent {
  type EC[_] = ExecutionContext
  type S[_] = Scheduler

  def tryToFuture[T]( t: => Try[T] ): Future[T] = {
    t match {
      case Success( s )  => Future successful { s }
      case Failure( ex ) => Future failed { ex }
    }
  }

  /**
    * Adapted from Verizon's Delorean library to provide correct and stable conversions between Future to Task.
    *
    * As a general rule of thumb:
    *
    * {{{
    *   def toTask[A](f: Future[A]) = Task { Await.result(f, Duration.Inf) }
    * }}}
    *
    * Obviously, the above is misleading since the definitions of these functions are non-blocking, but this should give you an
    * idea of their rough semantics.
    *
    * There are a few subtle things that are hidden here by the pretty syntax.
    *
    * $ - toTask is lazy in the Future parameter, meaning that evaluation of the method receiver will be deferred. This is
    * significant as Future is eagerly evaluated and caching. It is ideal to ensure that you do not accidentally eagerly
    * evaluate Future values before they are fed into toTask (note: we break this rule in the above example, since f
    * is a val)
    *
    * $ - The Task resulting from toTask will have sane semantics (i.e. it will behave like a normal Task) with
    * respect to reevaluation and laziness, provided that you are disciplined and ensure that the Future value that is the
    * dispatch receiver is not eagerly evaluated elsewhere (e.g. in a val). This means that you can reason about the results of
    * toTask in a referentially transparent fashion, as if the constituent Future had been constructed as a Task
    * all along.
    *
    * $ - toTask takes an implicit ExecutionContext and an implicit Scheduler. You should make
    * sure that the appropriate values of each are in scope. If you are not overriding Monix's default implicit Scheduler
    * for the rest of your Task composition, then you can just rely on the default Scheduler. The point is just to
    * make sure that the Task resulting from toTask is context-shifted into the same thread pool you're using for
    * normal Task(s).
    *
    * $ - toTask and unsafeToFuture are not strict inverses. They add overhead and defeat fairness algorithms in
    * both scala.concurrent and monix. So… uh… don't repeatedly convert back and forth, k?
    *
    * There is a major pitfall here. The toTask conversion can only give you a referentially transparent Task if you
    * religiously avoid eagerly caching its dispatch receiver. As a general rule, this isn't a bad idiom:
    *
    * {{{
    *   def f: Future[A] = ???
    *   val t: Task[A] = f.toTask
    * }}}
    *
    * In other words, f is a def and not a val. With this sort of machinery, toTask will give you a
    * reasonable output. If you eagerly cache its input Future though, the results are on your own head.
    */
  implicit class FutureAPI[A]( self: => Future[A] ) {
    def toTask(): Task[A] = Task fromFuture self
  }

  /**
    * Adapted from Verizon's Delorean library to provide correct and stable conversions between Future to Task.
    *
    * As a general rule of thumb:
    *
    * {{{
    *   def unsafeToFuture[A](t: Task[A]) = Future { t.run }
    * }}}
    *
    * Obviously, the above is misleading since the definitions of these functions are non-blocking, but this should give you an
    * idea of their rough semantics.
    *
    * There are a few subtle things that are hidden here by the pretty syntax.
    *
    * $ - unsafeToFuture is exactly as unsafe as it sounds, since the Future you get back will be running eagerly.
    * Do not make use of this function unless you are absolutely sure of what you're doing! It is very dangerous, because it
    * defeats the expected laziness of your Task computation. This function is meant primarily for interop with legacy
    * libraries that require values of type Future.
    *
    * $ - toTask and unsafeToFuture are not strict inverses. They add overhead and defeat fairness algorithms in
    * both scala.concurrent and monix. So… uh… don't repeatedly convert back and forth, k?
    *
    * There is a major pitfall here. The unsafeToFuture immediately runs the input Task (as mentioned above). There
    * really isn't anything else that could be done here, since Future is eager, but it's worth mentioning. Additionally,
    * unsafeToFuture makes no attempt to thread-shift the input Task, since in general this is not possible (or
    * necessarily desirable). The resulting Future will be on the appropriate thread pool, and it is certainly safe to
    * flatMap on said Future and treat it normally, but the computation itself will be run on whatever thread
    * scheduler the Task was composed against.
    */
  implicit class TaskAPI[A]( val self: Task[A] ) extends AnyVal {
    def unsafeToFuture[_: S](): Future[A] = self.runAsync
  }
}
