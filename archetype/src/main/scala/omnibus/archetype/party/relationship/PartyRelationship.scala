package omnibus.archetype.party.relationship

import scala.concurrent.Future
import omnibus.archetype.party.Party


abstract class PartyRelationship[CP <: Party, SP <: Party] {
  type Client = CP
  type ClientRole <: PartyRoleLike[CP]
  type Supplier = SP
  type SupplierRole <: PartyRoleLike[SP]
  type RelationshipType

  def relationshipType: RelationshipType
  def client: Client
  def clientRole: ClientRole
  def supplier: Supplier
  def supplierRole: SupplierRole
}


trait PartyRelationshipRepositoryComponent {

  def partyRelationshipRepository[CP <: Party, SP <: Party, PR <: PartyRelationship[CP, SP]]: PartyRelationshipRepository[CP, SP, PR]


  trait PartyRelationshipRepository[CP <: Party, SP <: Party, PR <: PartyRelationship[CP, SP]] {
    type Relationship = PR
    type ClientParty = CP
    type ClientRoleType <: PartyRoleType#Value
    type ClientPartyRole = PartyRoleSpecification[ClientParty, ClientRoleType]
    type SupplierParty = SP
    type SupplierRoleType <: PartyRoleType#Value
    type SupplierPartyRole = PartyRoleSpecification[SupplierParty, SupplierRoleType]
    type SuccessCount = Int

    def findRelationshipsForClient( roleSpec: ClientPartyRole ): Future[Seq[Relationship]]
    def findRelationshipsForSupplier( roleSpec: SupplierPartyRole ): Future[Seq[Relationship]]
  }
}
