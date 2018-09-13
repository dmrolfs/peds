package omnibus.archetype.domain.model.core

import org.joda.{ time => joda }
import omnibus.commons.util.Clock
import omnibus.identifier.Identifying

abstract class AuditedEntity[E <: AuditedEntity[E, ID], ID](
  implicit override protected val identifying: Identifying.Aux[E, ID]
) extends Entity[E, ID] {
  def audit: Option[AuditRecord]
}

trait AuditRecord {
  type ATID
  def createdBy: ATID
  def createdOn: joda.DateTime
  def modifiedBy: ATID
  def modifiedOn: joda.DateTime
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
