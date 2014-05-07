package peds.archetype

import org.joda.time.{DateTime, Interval, LocalDate}
import org.scala_tools.time.Imports._


trait Effectivity {
  /**
   * Inclusive start date this object is effective.
   */
  def validFrom: Option[DateTime]

  /**
   * Inclusive end date this object is effective. If validTo is None, the object is in effect.
   */
  def validTo: Option[DateTime]

  def effectiveOn( date: => LocalDate = { LocalDate.now } ): Boolean = effective( date.toDateTimeAtStartOfDay )

  def effective( datetime: => DateTime = { DateTime.now } ): Boolean = {
    (validFrom, validTo) match {
      case (None, None) => true
      case (Some(f), None) => ( f <= datetime )
      case (None, Some(t)) => ( datetime < t )
      case (Some(f), Some(t)) => ( f <= datetime ) && ( datetime < t )
    }
  }

  def isExpired( datetime: => DateTime = { DateTime.now } ): Boolean = validTo map { _ <= datetime } getOrElse false
}


case class Enablement( 
  enabled: Boolean, 
  override val validFrom: Option[DateTime] = None, 
  override val validTo: Option[DateTime] = None
) extends Effectivity {
  require( 
    !validFrom.isDefined || !validTo.isDefined || (validFrom.get <= validTo.get), 
    "enablement cannot be valid to a date before its start" 
  )
  
  override def toString: String = s"""(${if ( enabled ) "enabled" else "disabled"} for [${validFrom getOrElse ""}, ${validTo getOrElse ""}))"""
}
