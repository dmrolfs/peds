package omnibus.archetype.domain.model.core

import org.joda.{time => joda}
import omnibus.commons.util.Clock


trait AuditedEntity extends Entity {
  def audit: Option[AuditRecord]
}


trait AuditRecord {
  type ID
  def createdBy: ID
  def createdOn: joda.DateTime
  def modifiedBy: ID
  def modifiedOn: joda.DateTime
}

object AuditRecord {
  type Aux[ID0] = AuditRecord { type ID = ID0 }
}

case class SimpleAuditRecord[ID0](
  override val createdBy: ID0,
  override val createdOn: joda.DateTime, 
  override val modifiedBy: ID0,
  override val modifiedOn: joda.DateTime 
) extends AuditRecord {
  def this( actorID: ID0, timestamp: joda.DateTime ) = this( actorID, timestamp, actorID, timestamp )
  override type ID = ID0
}

object SimpleAuditRecord {
  def apply[ID]( actorID: ID, timestamp: joda.DateTime ): SimpleAuditRecord[ID] = new SimpleAuditRecord( actorID, timestamp )
  def apply[ID]( actorID: ID )( implicit clock: Clock ): SimpleAuditRecord[ID] = SimpleAuditRecord( actorID, clock() )
}
