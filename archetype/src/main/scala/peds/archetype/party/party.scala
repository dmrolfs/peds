package peds.archetype.party

import org.joda.time.LocalDate
import peds.archetype.{Address, OrganizationName, PersonName}
import relationship.{PartyRole, PartyRoleLike}


trait Party extends Equals {
  type ID

  def id: Option[ID]
  def name: String
  def addresses: Seq[Address]
  // def roles: Seq[PartyRoleLike[ID]]
  def roles: Seq[PartyRole]
  // def preferences: Seq[Preference]

  def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Party]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: Party => {
      if ( this eq that ) true
      else {
        ( that.## == this.## ) &&
        ( that canEqual this ) &&
        ( this.id == that.id )
      }
    }

    case _ => false
  }

  override def hashCode: Int = 41 * ( 41 + id.## )

  override def toString: String = s"""${getClass.getSimpleName}(id-${id}: ${name})"""
}


trait Person extends Party { 
  def personName: PersonName

  def otherPersonNames: Seq[PersonName] = Seq()

  override def name: String = personName.toString

  def dateOfBirth: Option[LocalDate] = None

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Person]
}


trait Organization extends Party {
  def organizationName: OrganizationName

  def otherOrganizationNames: Seq[OrganizationName] = Seq()

  override def name: String = organizationName.toString

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Organization]
}


trait Company extends Organization
