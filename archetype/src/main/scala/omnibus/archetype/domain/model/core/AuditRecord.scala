package omnibus.archetype.domain.model.core

import org.joda.{ time => joda }
import omnibus.commons.util.Clock

trait AuditedEntity extends EntityLike {
  def audit: Option[AuditRecord]
}

trait AuditRecord {
  type ATID
  def createdBy: ATID
  def createdOn: joda.DateTime
  def modifiedBy: ATID
  def modifiedOn: joda.DateTime
}

object AuditRecord {
  type Aux[ATID0] = AuditRecord { type ATID = ATID0 }
}

case class SimpleAuditRecord[ATID0](
  override val createdBy: ATID0,
  override val createdOn: joda.DateTime,
  override val modifiedBy: ATID0,
  override val modifiedOn: joda.DateTime
) extends AuditRecord {
  def this( actorID: ATID0, timestamp: joda.DateTime ) =
    this( actorID, timestamp, actorID, timestamp )
  override type ATID = ATID0
}

object SimpleAuditRecord {

  def apply[ATID]( actorID: ATID, timestamp: joda.DateTime ): SimpleAuditRecord[ATID] =
    new SimpleAuditRecord( actorID, timestamp )

  def apply[ATID]( actorID: ATID )( implicit clock: Clock ): SimpleAuditRecord[ATID] =
    SimpleAuditRecord( actorID, clock() )
}
