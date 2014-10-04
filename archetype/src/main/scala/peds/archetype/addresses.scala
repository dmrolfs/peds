package peds.archetype

import scala.collection.mutable
import java.util.Locale
import org.joda.time.DateTime


trait Address extends Effectivity with Equals {
  def address: String

  def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[Address]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: Address => {
      if ( this eq that ) true
      else {
        ( that.## == this.## ) &&
        ( that canEqual this ) &&
        ( this.address == that.address )
      }
    }

    case _ => false
  }

  override def hashCode: Int = 41 * ( 41 + address.## )

  override def toString: String = address
}


case class EmailAddress( 
  val email: String, 
  override val validFrom: Option[DateTime] = None, 
  override val validTo: Option[DateTime] = None
) extends Address {
  override def address: String = email

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[EmailAddress]
}


case class PostalCode( value: String )

case class GeographicAddress( 
  val addressLines: Seq[String], 
  val city: String, 
  val regionOrState: String, 
  val postalCode: PostalCode, 
  val country: String,
  val description: Option[String] = None,
  val locale: Option[Locale] = None,
  override val validFrom: Option[DateTime] = None, 
  override val validTo: Option[DateTime] = None
) extends Address {
  override def address: String = {
    val elements = addressLines ++ List( city, regionOrState, postalCode ) ++ List( country )
    elements mkString ","
  }

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[GeographicAddress]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: GeographicAddress => {
      if ( this eq that ) true
      else {
        ( that.## == this.## ) &&
        ( that canEqual this ) &&
        ( this.addressLines == that.addressLines ) &&
        ( this.city == that.city ) &&
        ( this.regionOrState == that.regionOrState ) &&
        ( this.postalCode == that.postalCode ) &&
        ( this.country == that.country )
      }
    }

    case _ => false
  }
  
  override def hashCode: Int = {
    41 * (
      41 * (
        41 * (
          41 * (
            41 + addressLines.##
          ) + city.##
        ) + regionOrState.##
      ) + postalCode.##
    ) + country.##
  }
}


case class GeocodeAddress( 
  val latitude: Double, 
  val longitude: Double, 
//dmr: add height and crs if needed but optional w default to None
  // val height: Option[Double] = None, 
  // val crsID: Option[String] = None,
  override val validFrom: Option[DateTime] = None, 
  override val validTo: Option[DateTime] = None
) extends Address {
  override def address: String = f"$latitude%+f $longitude%+f"

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[GeocodeAddress]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: GeocodeAddress => {
      if ( this eq that ) true
      else {
        ( that.## == this.## ) &&
        ( that canEqual this ) &&
        ( this.latitude == that.latitude ) &&
        ( this.longitude == that.longitude )
      }
    }

    case _ => false
  }
  
  override def hashCode: Int = {
    41 * (
      41 + latitude.##
    ) + longitude.##
  }
}


case class WebPageAddress( 
  val url: String, 
  override val validFrom: Option[DateTime] = None, 
  override val validTo: Option[DateTime] = None
) extends Address {
  override def address: String = url

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[WebPageAddress]
}


case class TelecomAddress( 
  val areaCode: String, 
  val number: String, 
  val extension: Option[String], 
  val physicalType: String, 
  val locale: Locale = Locale.US, 
  override val validFrom: Option[DateTime] = None, 
  override val validTo: Option[DateTime] = None
) extends Address {
  override def address: String = {
    import TelecomAddress._
    val countryCode = locale2CountryCode( locale ).map{ cd => if ( cd.isEmpty ) cd else ( cd + " " ) }
    val result = new mutable.StringBuilder( countryCode getOrElse "" )
    result append number
    extension.map{ " x" + _ }.foreach{ result append _ }
    result.toString
  }

  override def canEqual( rhs: Any ): Boolean = rhs.isInstanceOf[TelecomAddress]

  override def equals( rhs: Any ): Boolean = rhs match {
    case that: TelecomAddress => {
      if ( this eq that ) true
      else {
        ( that.## == this.## ) &&
        ( that canEqual this ) &&
        ( this.areaCode == that.areaCode ) &&
        ( this.number == that.number ) &&
        ( this.extension == that.extension ) &&
        ( this.locale == that.locale )
      }
    }

    case _ => false
  }
  
  override def hashCode: Int = {
    41 * (
      41 * (
        41 * (
          41 + areaCode.##
        ) + number.##
      ) + extension.##
    ) + locale.##
  }
}

object TelecomAddress {
  def locale2CountryCode( locale: Locale ): Option[String] = locale2CountryCode( locale.getCountry )

  def locale2CountryCode( isoCountry: String ): Option[String] = localePhoneCountryCodes get isoCountry

  // TODO: convert to use loaded csv file
  private val localePhoneCountryCodes: Map[String, String] = Map(
    "AF" -> "+93", 
    "AL" -> "+355", 
    "DZ" -> "+213", 
    "AD" -> "+376", 
    "AO" -> "+244", 
    "AG" -> "+1-268", 
    "AR" -> "+54", 
    "AM" -> "+374", 
    "AU" -> "+61", 
    "AT" -> "+43", 
    "AZ" -> "+994", 
    "BS" -> "+1-242", 
    "BH" -> "+973", 
    "BD" -> "+880", 
    "BB" -> "+1-246", 
    "BY" -> "+375", 
    "BE" -> "+32", 
    "BZ" -> "+501", 
    "BJ" -> "+229", 
    "BT" -> "+975", 
    "BO" -> "+591", 
    "BA" -> "+387", 
    "BW" -> "+267", 
    "BR" -> "+55", 
    "BN" -> "+673", 
    "BG" -> "+359", 
    "BF" -> "+226", 
    "BI" -> "+257", 
    "KH" -> "+855", 
    "CM" -> "+237", 
    "CA" -> "+1", 
    "CV" -> "+238", 
    "CF" -> "+236", 
    "TD" -> "+235", 
    "CL" -> "+56", 
    "CN" -> "+86", 
    "CO" -> "+57", 
    "KM" -> "+269", 
    "CD" -> "+243", 
    "CG" -> "+242", 
    "CR" -> "+506", 
    "CI" -> "+225", 
    "HR" -> "+385", 
    "CU" -> "+53", 
    "CY" -> "+357", 
    "CZ" -> "+420", 
    "DK" -> "+45", 
    "DJ" -> "+253", 
    "DM" -> "+1-767", 
    "DO" -> "+1-809and1-829", 
    "EC" -> "+593", 
    "EG" -> "+20", 
    "SV" -> "+503", 
    "GQ" -> "+240", 
    "ER" -> "+291", 
    "EE" -> "+372", 
    "ET" -> "+251", 
    "FJ" -> "+679", 
    "FI" -> "+358", 
    "FR" -> "+33", 
    "GA" -> "+241", 
    "GM" -> "+220", 
    "GE" -> "+995", 
    "DE" -> "+49", 
    "GH" -> "+233", 
    "GR" -> "+30", 
    "GD" -> "+1-473", 
    "GT" -> "+502", 
    "GN" -> "+224", 
    "GW" -> "+245", 
    "GY" -> "+592", 
    "HT" -> "+509", 
    "HN" -> "+504", 
    "HU" -> "+36", 
    "IS" -> "+354", 
    "IN" -> "+91", 
    "ID" -> "+62", 
    "IR" -> "+98", 
    "IQ" -> "+964", 
    "IE" -> "+353", 
    "IL" -> "+972", 
    "IT" -> "+39", 
    "JM" -> "+1-876", 
    "JP" -> "+81", 
    "JO" -> "+962", 
    "KZ" -> "+7", 
    "KE" -> "+254", 
    "KI" -> "+686", 
    "KP" -> "+850", 
    "KR" -> "+82", 
    "KW" -> "+965", 
    "KG" -> "+996", 
    "LA" -> "+856", 
    "LV" -> "+371", 
    "LB" -> "+961", 
    "LS" -> "+266", 
    "LR" -> "+231", 
    "LY" -> "+218", 
    "LI" -> "+423", 
    "LT" -> "+370", 
    "LU" -> "+352", 
    "MK" -> "+389", 
    "MG" -> "+261", 
    "MW" -> "+265", 
    "MY" -> "+60", 
    "MV" -> "+960", 
    "ML" -> "+223", 
    "MT" -> "+356", 
    "MH" -> "+692", 
    "MR" -> "+222", 
    "MU" -> "+230", 
    "MX" -> "+52", 
    "FM" -> "+691", 
    "MD" -> "+373", 
    "MC" -> "+377", 
    "MN" -> "+976", 
    "ME" -> "+382", 
    "MA" -> "+212", 
    "MZ" -> "+258", 
    "MM" -> "+95", 
    "NA" -> "+264", 
    "NR" -> "+674", 
    "NP" -> "+977", 
    "NL" -> "+31", 
    "NZ" -> "+64", 
    "NI" -> "+505", 
    "NE" -> "+227", 
    "NG" -> "+234", 
    "NO" -> "+47", 
    "OM" -> "+968", 
    "PK" -> "+92", 
    "PW" -> "+680", 
    "PA" -> "+507", 
    "PG" -> "+675", 
    "PY" -> "+595", 
    "PE" -> "+51", 
    "PH" -> "+63", 
    "PL" -> "+48", 
    "PT" -> "+351", 
    "QA" -> "+974", 
    "RO" -> "+40", 
    "RU" -> "+7", 
    "RW" -> "+250", 
    "KN" -> "+1-869", 
    "LC" -> "+1-758", 
    "VC" -> "+1-784", 
    "WS" -> "+685", 
    "SM" -> "+378", 
    "ST" -> "+239", 
    "SA" -> "+966", 
    "SN" -> "+221", 
    "RS" -> "+381", 
    "SC" -> "+248", 
    "SL" -> "+232", 
    "SG" -> "+65", 
    "SK" -> "+421", 
    "SI" -> "+386", 
    "SB" -> "+677", 
    "SO" -> "+252", 
    "ZA" -> "+27", 
    "ES" -> "+34", 
    "LK" -> "+94", 
    "SD" -> "+249", 
    "SR" -> "+597", 
    "SZ" -> "+268", 
    "SE" -> "+46", 
    "CH" -> "+41", 
    "SY" -> "+963", 
    "TJ" -> "+992", 
    "TZ" -> "+255", 
    "TH" -> "+66", 
    "TL" -> "+670", 
    "TG" -> "+228", 
    "TO" -> "+676", 
    "TT" -> "+1-868", 
    "TN" -> "+216", 
    "TR" -> "+90", 
    "TM" -> "+993", 
    "TV" -> "+688", 
    "UG" -> "+256", 
    "UA" -> "+380", 
    "AE" -> "+971", 
    "GB" -> "+44", 
    "US" -> "+1", 
    "UY" -> "+598", 
    "UZ" -> "+998", 
    "VU" -> "+678", 
    "VA" -> "+379", 
    "VE" -> "+58", 
    "VN" -> "+84", 
    "YE" -> "+967", 
    "ZM" -> "+260", 
    "ZW" -> "+263", 
    "GE" -> "+995", 
    "TW" -> "+886", 
    "AZ" -> "+374-97", 
    "CY" -> "+90-392", 
    "MD" -> "+373-533", 
    "SO" -> "+252", 
    "GE" -> "+995", 
    "AU" -> "", 
    "CX" -> "+61", 
    "CC" -> "+61", 
    "AU" -> "", 
    "HM" -> "", 
    "NF" -> "+672", 
    "NC" -> "+687", 
    "PF" -> "+689", 
    "YT" -> "+262", 
    "GP" -> "+590", 
    "PM" -> "+508", 
    "WF" -> "+681", 
    "TF" -> "", 
    "PF" -> "", 
    "BV" -> "", 
    "CK" -> "+682", 
    "NU" -> "+683", 
    "TK" -> "+690", 
    "GG" -> "+44", 
    "IM" -> "+44", 
    "JE" -> "+44", 
    "AI" -> "+1-264", 
    "BM" -> "+1-441", 
    "IO" -> "+246", 
    "VG" -> "+1-284", 
    "KY" -> "+1-345", 
    "FK" -> "+500", 
    "GI" -> "+350", 
    "MS" -> "+1-664", 
    "PN" -> "", 
    "SH" -> "+290", 
    "GS" -> "", 
    "TC" -> "+1-649", 
    "MP" -> "+1-670", 
    "PR" -> "+1-787and1-939", 
    "AS" -> "+1-684", 
    "UM" -> "", 
    "GU" -> "+1-671", 
    "VI" -> "+1-340", 
    "HK" -> "+852", 
    "MO" -> "+853", 
    "FO" -> "+298", 
    "GL" -> "+299", 
    "GF" -> "+594", 
    "GP" -> "+590", 
    "MQ" -> "+596", 
    "RE" -> "+262", 
    "AX" -> "+358-18", 
    "AW" -> "+297", 
    "AN" -> "+599", 
    "SJ" -> "+47", 
    "AC" -> "+247", 
    "TA" -> "+290", 
    "AQ" -> "", 
    "CS" -> "+381", 
    "PS" -> "+970", 
    "EH" -> "+212"
  )
}
