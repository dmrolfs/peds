package peds.commons.util


object Chain {
  type Link[A, B] = A => Either[A, B]
  implicit def func2Chain[A, B]( f: Link[A, B] ): Chain[A, B] = new Chain[A, B]( f )
}


/**
 * Implements a functional version of the Chain of Responsibility pattern.  The Chain of Responsibility pattern is a GoF design 
 * pattern consisting of a source of command objects and a series of processing objects. Each processing object contains logic 
 * that defines the types of command objects that it can handle; the rest are passed to the next processing object in the chain. 
 *
 * In this Scala version, processing steps are defined as functions, which the Chain links together via either the chainWith() or
 * +> operations. The return value is defined as an Either[A, B]:
 * - Left[A] to direct processing by the next handler
 * - Right[B] to direct that processing must stop with the preceeding result.
 */

//todo: change to Kleisli
 
class Chain[A, B]( f: Chain.Link[A, B] ) {
  
  def chainWith( next: => Chain.Link[A, B] ): Chain.Link[A, B] = f( _ ).left.flatMap( next ) // call next handler if not yet processed

  def +>( next: => Chain.Link[A, B] ): Chain.Link[A, B] = chainWith( next )
}
