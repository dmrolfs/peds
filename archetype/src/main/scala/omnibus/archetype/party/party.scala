package omnibus.archetype.party

import org.joda.time.LocalDate
import omnibus.archetype.domain.model.core.Entity
import omnibus.archetype.{ Address, OrganizationName, PersonName }
import relationship.PartyRole
import omnibus.core.syntax.clazz._
import omnibus.identifier.Identifying

//todo: rethink all of archetype in terms of Aux pattern and functional free monads or Reader monad
abstract class Party[P <: Party[P, ID], ID](
  implicit override protected val identifying: Identifying.Aux[P, ID]
) extends Entity[P, ID]
    with Equals {
  def addresses: Seq[Address]
  // def roles: Seq[PartyRoleLike[ID]]
  def roles: Seq[PartyRole]
  // def preferences: Seq[Preference]

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Party[P, _]]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: Party[P, _] => {
      if (this eq that) true
      else {
        (that.## == this.##) &&
        (that canEqual this) &&
        (this.id == that.id)
      }
    }

    case _ => false
  }

  override def hashCode: Int = 41 * (41 + id.##)

  override def toString: String = s"""${getClass.safeSimpleName}(id-${id}:${name})"""
}

abstract class Person[P <: Person[P, ID], ID](
  implicit override protected val identifying: Identifying.Aux[P, ID]
) extends Party[P, ID] {
  def personName: PersonName

  def otherPersonNames: Seq[PersonName] = Seq()

  override def name: String = personName.toString

  def dateOfBirth: Option[LocalDate] = None

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Person[P, _]]
}

abstract class Organization[O <: Organization[O, ID], ID](
  implicit override protected val identifying: Identifying.Aux[O, ID]
) extends Party[O, ID] {
  def organizationName: OrganizationName

  def otherOrganizationNames: Seq[OrganizationName] = Seq()

  override def name: String = organizationName.toString

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Organization[O, _]]
}

abstract class Company[C <: Company[C, ID], ID](
  implicit override protected val identifying: Identifying.Aux[C, ID]
) extends Organization[C, ID]
