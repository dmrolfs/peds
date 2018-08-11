package omnibus.archetype

import scala.collection.mutable
import org.joda.time.DateTime

//todo: rethink all of archetype in terms of Aux pattern and functional free monads or Reader monad
case object NameUsage extends Enumeration {
  type NameUsage = Value
  val Legal = Value( 1, "legal" )
  val Alias = Value( 2, "alias" )
}
import NameUsage._

case class PersonName(
  val familyName: String,
  val givenName: String,
  val middleName: Option[String] = None,
  val prefix: Option[String] = None,
  val suffix: Option[String] = None,
  val preferredName: Option[String] = None,
  val use: Option[NameUsage] = None,
  override val validFrom: Option[DateTime] = None,
  override val validTo: Option[DateTime] = None
) extends Effectivity
    with Equals {

  def fullName: String = {
    val pre = prefix.map { _ + " " } getOrElse ""
    val result = new mutable.StringBuilder( pre )
    result append givenName
    result append " "
    middleName.map { _ + " " }.foreach { result append _ }
    result append familyName
    suffix.map { ", " + _ }.foreach { result append _ }
    result.toString
  }

  def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[PersonName]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: PersonName => {
      if (this eq that) true
      else {
        (that.## == this.##) &&
        (that canEqual this) &&
        (this.familyName == that.familyName) &&
        (this.givenName == that.givenName) &&
        (this.middleName == that.middleName)
      }
    }

    case _ => false
  }

  override def hashCode: Int = {
    41 * (
      41 * (
        41 + familyName.##
      ) + givenName.##
    ) + middleName.##
  }

  override def toString: String = fullName
}

object OrganizationName {
  val Undefined: OrganizationName = OrganizationName( name = "" )
}

case class OrganizationName(
  val name: String,
  val use: Option[NameUsage] = None,
  override val validFrom: Option[DateTime] = None,
  override val validTo: Option[DateTime] = None
) extends Effectivity
    with Equals {
  def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[OrganizationName]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: OrganizationName => {
      if (this eq that) true
      else {
        (that.## == this.##) &&
        (that canEqual this) &&
        (this.name == that.name)
      }
    }

    case _ => false
  }

  override def hashCode: Int = 41 * (41 + name.##)

  override def toString: String = name
}
