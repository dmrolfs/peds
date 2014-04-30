package peds.archetype.domain.model.core

// import scala.concurrent.Future
import org.joda.{time => joda}
// import rillit._
import peds.commons.Clock


trait AuditedEntity extends Entity {
  def audit: Option[AuditRecord]
}


trait AuditRecord {
  type UID
  def createdBy: UID
  def createdOn: joda.DateTime
  def modifiedBy: UID
  def modifiedOn: joda.DateTime
}

case class SimpleAuditRecord[ID]( 
  override val createdBy: ID,
  override val createdOn: joda.DateTime, 
  override val modifiedBy: ID, 
  override val modifiedOn: joda.DateTime 
) extends AuditRecord {
  def this( actorID: ID, timestamp: joda.DateTime ) = this( actorID, timestamp, actorID, timestamp )
  override type UID = ID
}

object SimpleAuditRecord {
  def apply[UID]( actorID: UID, timestamp: joda.DateTime ): SimpleAuditRecord[UID] = new SimpleAuditRecord( actorID, timestamp )
  def apply[UID]( actorID: UID )( implicit clock: Clock ): SimpleAuditRecord[UID] = SimpleAuditRecord( actorID, clock() )
}
