package omnibus.archetype.party.relationship

import omnibus.archetype.party.Party


trait PartyRole


trait PartyRoleLike[P <: Party] extends PartyRole {
  type PartyId = P#ID
  type RoleType <: PartyRoleType#Value

  def partyId: PartyId
  def roleType: RoleType
} 


case class PartyRoleSpecification[P <: Party, R <: PartyRoleType#Value]( 
  override val partyId: P#ID, 
  override val roleType: R 
) extends PartyRoleLike[P] {
  override type RoleType = R
}
