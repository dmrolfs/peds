package omnibus.lagom

import java.io.Serializable
import akka.annotation.DoNotInherit

/**
  * Typically used together with `Future` to signal acknolwedgement
  * but there is no actual value completed. More clearly signals intent
  * than `Unit` and is available both from Scala and Java (which `Unit` is not).
  */
@DoNotInherit sealed abstract class Acknowledged extends Serializable

case object Acknowledged extends Acknowledged {

  /**
    * Java API: the singleton instance
    */
  def getInstance(): Acknowledged = this

  /**
    * Java API: the singleton instance
    *
    * This is equivalent to [[Acknowledged#getInstance()]], but can be used with static import.
    */
  def acknowledged(): Acknowledged = this
}
