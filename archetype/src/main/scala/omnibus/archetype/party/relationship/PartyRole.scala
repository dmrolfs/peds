package omnibus.archetype.party.relationship

import scala.language.existentials
import omnibus.archetype.party.Party

//todo: rethink all of archetype in terms of Aux pattern and functional free monads or Reader monad

trait PartyRole

abstract class PartyRoleLike[P <: Party[P, ID], ID] extends PartyRole {
  type PartyId = P#TID
  type RoleType <: PartyRoleType#Value

  def partyId: PartyId
  def roleType: RoleType
}

case class PartyRoleSpecification[P <: Party[P, ID], ID, R <: PartyRoleType#Value](
  override val partyId: P#TID,
  override val roleType: R
) extends PartyRoleLike[P, ID] {
  override type RoleType = R
}
