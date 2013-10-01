package peds.archetype

import org.joda.{time => joda}


trait Entity {
  type ID
  type ActorID

  def id: Option[ID]
  def createdBy: Option[ActorID]
  def createdOn: Option[joda.DateTime]
  def modifiedBy: Option[ActorID]
  def modifiedOn: Option[joda.DateTime]
}