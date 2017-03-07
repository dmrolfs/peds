package omnibus.commons.concurrent

import scala.concurrent.{ Future, ExecutionContext }


/** ported from Practical Future[Option[A]] in Scala -> http://edofic.com/posts/2014-03-07-practical-future-option.html
 * Motivation
 *
 * In real world concurrent code you often come across the Future[Option[A]] type(where A is usually some concrete type). 
 * And then you need to compose these things. This is not straightforward in Scala. 
 *
 * If you’ve done some Haskell just import scalaz._ and you can skip the rest of this article. Scalaz library defines a monad 
 * typeclass(and many others) that formally specifies what it means to be a monad(not “has a flatMap-ish thingy”). Then it’s 
 * easy to build abstractions upon this.
 *
 * But what if you don’t want to add another dependency and just want to make this tiny bit more practical? I can be done and is
 * not that complicated. You will also learn something and maybe even become motivated to bite the bullet and start using Scalaz.
 *
 * Meaning
 *
 * The Future[Option[A]] type combines the notion of concurrency(from Future) and the notion of failure(from Option) giving you
 * a concurrent computation that might fail. What we want is a monad instance for this. With only standard library code you 
 * probably would use Future#flatMap in combination with Option#map and Option#getOrElse(or just Option#fold). This gets messy 
 * and unreadable quite quickly due to loads of boilerplate. 
 *
 * Newtyping
 *
 * A monad in scala means a flatMap method, so it’s safe to assume we need to define a flatMap with this special semantics 
 * somewhere. You might want to define an implicit class that has the new method but it wouldn’t work as Future already has a 
 * flatMap method. I took a page from Haskell’s book. When you want to override semantics there you wrap up the type into a 
 * newtype. This is just compile-time type information that is completely free at runtime. Luckily Scala has this in form of 
 * AnyVal.
 *
 * Usage
 *
 * What good is this FutureO? Let’s do a contrived example. We need a function that might fail - divideEven that only divides 
 * even numbers. And we need concurrency - we’ll divide two numbers concurrently.
 * {{{
 * def divideEven(n: Int): Option[Int] = 
 *     if (n % 2 == 0) Some(n/2) else None
 * 
 * //first spawn both computations
 * val f1 = Future(divideEven(14))
 * val f2 = Future(divideEven(16))
 * 
 * //and combine them
 * val fc = for {
 *     a <- FutureO(f1)
 *     b <- FutureO(f2)
 * } yield a + b
 * 
 * //prints out Success(Some(15))
 * fc.future onComplete println
 * It works. How would this look without FutureO?
 * 
 * val fc = for {
 *     oa <- f1
 *     ob <- f2
 * } yield for {
 *     a <- oa
 *     b <- ob
 * } yield a + b
 * 
 * fc onComplete println
 * }}}
 * Two layers of for comprehensions. It gets even hairier if you have data dependencies between your futures. Consider divideEven
 * again but this time we want to divide a number twice in a row. And we’ll be keeping the futures around just to prove a point.
 * Let’s imagine that divideEven does some blocking IO and we want to push it into another tread-pool.
 * 
 * {{{
 * def divideTwiceF(n: Int): Future[Option[Int]] = {
 *     val fo = for {
 *         n1 <- FutureO(Future(divideEven(n)))
 *         n2 <- FutureO(Future(divideEven(n1)))
 *     } yield n2
 *     fo.future
 * }
 * }}}
 * And it works as expected. The FutureO part there is just to alter the monadic semantics. As an exercise try to rewrite this
 * without FutureO and squirm in disgust.
 * 
 * Usability
 * 
 * Inside for comprehension(or manual flatMaps) you can still construct failed futures, throw or return None(in a future). 
 * However putting in a default value(getOrElse) is a bit trickier and going back to regular Future inside same comprehension is
 * impossible. But you can fix this. You can define methods like orElse on the FutureO. You can also overload the flatMap to
 * enable interop with regular futures. However this screws up type inference and I would advise against it as it could introduce
 * some nasty bugs.
 * 
 * Try to implement combinators you need and leave some comments. Especially if you come across something nice or find a case
 * where FutureO is more awkward to use than regular futures.
 */
case class FutureO[+A]( future: Future[Option[A]] ) extends AnyVal {
  def flatMap[B]( f: A => FutureO[B] )( implicit ec: ExecutionContext ): FutureO[B] = {
    FutureO { future.flatMap { optA => optA.map { a => f( a ).future } getOrElse Future.successful( None ) } }
  }

  def map[B]( f: A => B )( implicit ec: ExecutionContext ): FutureO[B] = FutureO( future.map( _ map f ) )
}
