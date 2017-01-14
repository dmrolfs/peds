package peds.akka

import akka.actor.Actor


/**
 * Defines a trait supporting extending an Akka Actor's receive() via supreclasses and stackable traits without losing the 
 * extended behavior. ActorStack implementations should wrap the r: Receive with their behavior, and call 
 * {{{
 *   case msg => super.around( r )( msg )
 * }}}
 * in order to not hide upstream stack behavior.
 * 
 * Concrete Actors must wrap all Receive expressions with around(...) in order to retain the stacked extension; e.g.,
 * {{{
 *   case _: PostAdded => context become around( created )
 * }}}
 *
 * ActorStack does not override any other Actor behavior.
 */
trait ActorStack { outer: Actor =>
  /**
   * The around() method is used by Actor implementations to extend Actor.receive behavior through superclass and stacked traits.
   * This method must be used in all cases where it wants to retain the stack-extended behavior; otherwise the stacked
   * extensions will not be incorporated and not survive operations such as `context.become(...)`.
   */
  def around( r: Receive ): Receive = {
    case m if r.isDefinedAt( m ) => r( m )
    case m => unhandled( m )
  }
}
