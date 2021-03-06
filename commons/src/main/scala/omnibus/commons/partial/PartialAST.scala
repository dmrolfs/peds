package omnibus.commons.partial

import scala.util.parsing.combinator._

sealed abstract class PartialCriteria( val properties: Map[String, String] = Map() )
    extends Equals {
  def getProperty( property: String ): Option[String] = properties get property

  def get( field: String ): Option[PartialCriteria]
  def contains( field: String ): Boolean
  def isComposite: Boolean
  def withProperties: Map[String, PartialCriteria]

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[PartialCriteria]
  override def equals( rhs: Any ): Boolean = rhs match {
    case that: PartialCriteria => {
      if (this eq that) true
      else {
        (that.## == this.##) &&
        (that canEqual this) &&
        (properties == that.properties)
      }
    }
  }

  override def hashCode: Int = {
    41 * (
      41 + properties.hashCode
    )
  }
}

case class CompositeCriterion(
  private val props: Map[String, String],
  private val subCriteria: ( String, PartialCriteria )*
) extends PartialCriteria( props )
    with Equals {
  private val mySubCriteria: Map[String, PartialCriteria] = Map( subCriteria: _* )
  override def get( field: String ): Option[PartialCriteria] = mySubCriteria get field
  override def contains( field: String ): Boolean =
    mySubCriteria.isEmpty || (mySubCriteria contains field)
  override def isComposite: Boolean = true
  override def withProperties: Map[String, PartialCriteria] = mySubCriteria filter {
    !_._2.properties.isEmpty
  }

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[CompositeCriterion]
  override def equals( rhs: Any ): Boolean = {
    rhs match {
      case that: CompositeCriterion => {
        if (this eq that) true
        else {
          (that.## == this.##) &&
          (super.equals( that ) ) &&
          (that canEqual this) &&
          (mySubCriteria == that.mySubCriteria)
        }
      }

      case _ => false
    }
  }

  override def hashCode: Int = {
    41 * (
      41 + super.hashCode
    ) + mySubCriteria.hashCode
  }

  override def toString: String = {
    val subs = mySubCriteria.map { kv =>
      kv match {
        case ( field, prime: PrimeCriterion ) if prime.properties.isEmpty => field
        case ( field, prime: PrimeCriterion )                             => field + prime.properties.mkString( "[", ",", "]" )
        case ( field, composite: CompositeCriterion ) if composite.properties.isEmpty =>
          field + ":" + composite
        case ( field, composite: CompositeCriterion ) =>
          field + ":" + composite + composite.properties.mkString( "[", ",", "]" )
        case ( field, value ) => field + ":[[" + value + "]]" // don't expect this to ever be called
      }
    }
    subs.mkString( "(", ",", ")" )
  }
}

object CompositeCriterion {
  val empty: CompositeCriterion = new CompositeCriterion( Map() )
}

case class PrimeCriterion( private val props: Map[String, String] = Map() )
    extends PartialCriteria( props )
    with Equals {
  override def get( field: String ): Option[PartialCriteria] = None
  override def contains( field: String ): Boolean = true
  override def isComposite: Boolean = false
  override def withProperties: Map[String, PartialCriteria] = Map.empty

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[PrimeCriterion]
  override def equals( rhs: Any ): Boolean = {
    rhs match {
      case that: PrimeCriterion => {
        if (this eq that) true
        else {
          (that.## == this.##) &&
          (super.equals( that ) ) &&
          (that canEqual this)
        }
      }

      case _ => false
    }
  }
}

object PrimeCriterion {}

class PartialParser extends RegexParsers with PackratParsers {
  val field = """[\w_-]+""".r

  def parse( input: String ): PartialCriteria =
    CompositeCriterion( Map(), (parseAll( criteria, input ).getOrElse( Seq.empty ) ): _* )

  def criteria: Parser[Seq[( String, PartialCriteria )]] =
    rep1sep( criterion, "," ) | "(" ~> rep1sep( criterion, "," ) <~ ")"
  def criterion: Parser[( String, PartialCriteria )] = (composite | prime)

  def composite: Parser[( String, PartialCriteria )] =
    field ~ opt( properties ) ~ ":" ~ criteria ^^ {
      case f ~ Some( props ) ~ ":" ~ crit => f -> CompositeCriterion( Map( props: _* ), crit: _* )
      case f ~ None ~ ":" ~ crit          => f -> CompositeCriterion( Map(), crit: _* )
    }

  def prime: Parser[( String, PartialCriteria )] = field ~ opt( properties ) ^^ {
    case f ~ optProps => (f -> PrimeCriterion( Map( (optProps getOrElse Seq()): _* ) ))
  }

  def properties: Parser[Seq[( String, String )]] = rep1( property )
  def property: Parser[( String, String )] = sortAscProperty | sortDescProperty

  def sortAscProperty: Parser[( String, String )] =
    "+" ~> ("sort" | "sort-asc") ~> "=" ~> field ^^ { case f => "sort-asc" -> f }

  def sortDescProperty: Parser[( String, String )] = "+" ~> "sort-desc" ~> "=" ~> field ^^ {
    case f => "sort-desc" -> f
  }
}
