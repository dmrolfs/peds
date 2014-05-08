package peds.archetype.domain.model.core

import shapeless.Lens


trait ConcurrencySafety {
  def concurrencyVersion: Int

  // def concurrencyVersion_=( version: Int ): Unit = {
  //   failWhenConcurrencyViolation( version )
  //   doSetConcurrencyVersion( version )
  // }

  // protected def doSetConcurrencyVersion( version: Int ): Unit


  def failWhenConcurrencyViolation( version: Int ): Unit = {
    if ( version != concurrencyVersion ) {
      throw new IllegalStateException( s"Concurrency Violation: Stale data detected. Entity (${this.toString}) was already modified." )
    }
  } 
}
