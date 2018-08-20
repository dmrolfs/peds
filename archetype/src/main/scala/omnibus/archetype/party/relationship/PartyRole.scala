package omnibus.archetype.party.relationship

import scala.language.existentials
import omnibus.archetype.party.Party

//todo: rethink all of archetype in terms of Aux pattern and functional free monads or Reader monad

trait PartyRole

trait PartyRoleLike[P <: Party] extends PartyRole {
  type PartyId = P#TID
  type RoleType <: PartyRoleType#Value

  def partyId: PartyId
  def roleType: RoleType
}

case class PartyRoleSpecification[P <: Party, R <: PartyRoleType#Value](
  override val partyId: P#TID,
  override val roleType: R
) extends PartyRoleLike[P] {
  override type RoleType = R
}
