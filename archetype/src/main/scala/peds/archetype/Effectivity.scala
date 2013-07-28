package peds.archetype

import org.joda.time.{DateTime, Interval, LocalDate}
import org.scala_tools.time.Imports._


trait Effectivity {
  /**
   * Inclusive start date this object is effective.
   */
  def validFrom: Option[LocalDate]

  /**
   * Inclusive end date this object is effective. If validTo is None, the object is in effect.
   */
  def validTo: Option[LocalDate]

  def isActive( date: => LocalDate = { LocalDate.now } ): Boolean = isActive( date.toDateTimeAtStartOfDay )

  def isActive( datetime: DateTime ): Boolean = {
    val from = validFrom map { _.toDateTimeAtStartOfDay }
    val to = validTo map { _.toDateTimeAtStartOfDay + 1.day }
    (from, to) match {
      case (None, None) => true
      case (Some(f), None) => ( f <= datetime )
      case (None, Some(t)) => ( datetime < t )
      case (Some(f), Some(t)) => ( f <= datetime ) && ( datetime < t )
    }
  }
}