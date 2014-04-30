package peds.archetype.domain.model.core

// import scala.concurrent.Future
// import org.joda.{time => joda}
import rillit._
// import peds.commons.Clock


trait Entity {
  type That <: Entity

  type ID
  def idClass: Class[_]

  def id: Option[ID]
  def idLens: Lens[That, Option[ID]]

  def name: String
  def nameLens: Lens[That, String]
}

object Entity {
  implicit def summarizeEntity( e: Entity ): EntitySummary = EntitySummary( e )
}
