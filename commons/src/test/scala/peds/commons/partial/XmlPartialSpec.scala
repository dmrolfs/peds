package peds.commons.partial

import scala.xml._
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.StreamlinedXml
import com.typesafe.scalalogging.LazyLogging


class XmlPartialSpec() extends FlatSpec with Matchers with StreamlinedXml with LazyLogging {
  import XmlPartialSpec._

  "An elided XML API" should "filter simple list" in {
    (
      elide( myers, "inquiryId, orderId" ) === (
        <reply>
          <inquiryId>381e07f0-453d-11e2-b04c-22000a91952c</inquiryId>
          <orderId>564eaf86-11ae-4d22-b4e8-26bc00484b8c</orderId>
        </reply>
      )
    ) (after being streamlined[Elem])
  }

  it should "filter simple composite list" in {
    (
      elide( myers, "person:name:(given,family)" ) === (
        <reply><person><name><family>Myers</family><given>Michael</given></name></person></reply>
      )
    ) (after being streamlined[Elem])
  }

  it should "filter simple mix of prime and composite list" in {
    (
      elide( myers, "inquiryId,orderId,person:name:(given,family)" ) === (
        <reply>
          <inquiryId>381e07f0-453d-11e2-b04c-22000a91952c</inquiryId>
          <orderId>564eaf86-11ae-4d22-b4e8-26bc00484b8c</orderId>
          <person><name><family>Myers</family><given>Michael</given></name></person>
        </reply>
      )
    ) (after being streamlined[Elem])
  }

  it should "filter simple mix of prime and two simple composite list" in {
    (
      elide( myers, "inquiryId,orderId,person:(name:(given,family)),report:offender:address:addressLine" ) === (
        <reply>
          <inquiryId>381e07f0-453d-11e2-b04c-22000a91952c</inquiryId>
          <orderId>564eaf86-11ae-4d22-b4e8-26bc00484b8c</orderId>
          <person><name><family>Myers</family><given>Michael</given></name></person>
          <report>
            <offender><address><addressLine>204 MORNINGSIDE DR</addressLine></address></offender>
            <offender><address><addressLine>327 N WALLACE AVE</addressLine></address></offender>
            <offender><address><addressLine>227 N WALLACE AVE</addressLine></address></offender>
            <offender><address><addressLine>204 MORNINGSIDE DR</addressLine></address></offender>
          </report>
        </reply>
      )
    ) (after being streamlined[Elem])
  }

  it should "simple filter and sort" in {
    val data = <top><bar><alpha>c</alpha><foo>3</foo></bar><bar><foo>2</foo><alpha>b</alpha></bar><bar><alpha>a</alpha><foo>1</foo></bar><zed>do not include</zed></top>
    (
      elide( data, "bar" ) === ( 
        <top>
          <bar><alpha>c</alpha><foo>3</foo></bar>
          <bar><foo>2</foo><alpha>b</alpha></bar>
          <bar><alpha>a</alpha><foo>1</foo></bar>
        </top> 
      ) 
    ) (after being streamlined[Elem])

    (
      ( elide( data, "bar+sort=foo" ) \ "bar" ) === (
        (
          <top>
            <bar><alpha>a</alpha><foo>1</foo></bar>
            <bar><foo>2</foo><alpha>b</alpha></bar>
            <bar><alpha>c</alpha><foo>3</foo></bar>
          </top> 
        ) \ "bar" 
      )
    ) (after being streamlined[NodeSeq])

    (
      ( elide( data, "bar+sort=foo:alpha" ) \ "bar" ) === ( 
        (
          <top>
            <bar><alpha>a</alpha></bar>
            <bar><alpha>b</alpha></bar>
            <bar><alpha>c</alpha></bar>
          </top> 
        ) \ "bar" 
      )
    ) (after being streamlined[NodeSeq])
  }

  it should "simple filter and sort-desc" in {
    val data = <top><bar><alpha>c</alpha><foo>3</foo></bar><bar><foo>2</foo><alpha>b</alpha></bar><bar><alpha>a</alpha><foo>1</foo></bar><zed>do not include</zed></top>
    (
      elide( data, "bar" ) === ( 
        <top>
          <bar><alpha>c</alpha><foo>3</foo></bar>
          <bar><foo>2</foo><alpha>b</alpha></bar>
          <bar><alpha>a</alpha><foo>1</foo></bar>
        </top> 
      )
    ) (after being streamlined[Elem])

    (
      ( elide( data, "bar+sort-desc=foo" ) \ "bar" ) === ( 
        (
          <top>
            <bar><alpha>c</alpha><foo>3</foo></bar>
            <bar><foo>2</foo><alpha>b</alpha></bar>
            <bar><alpha>a</alpha><foo>1</foo></bar>
          </top> 
        ) \ "bar" 
      )
    ) (after being streamlined[NodeSeq])

    (
      ( elide( data, "bar+sort-desc=foo:alpha" ) \ "bar" ) === ( 
        Utility.trim(
          <top>
            <bar><alpha>c</alpha></bar>
            <bar><alpha>b</alpha></bar>
            <bar><alpha>a</alpha></bar>
          </top> 
        ) \ "bar" 
      )
    ) (after being streamlined[NodeSeq])
  }

  it should "filter and sort" in {
    val actual = elide( myers, "report:offender+sort=fullname:address:addressLine" ) \\ "offender"
    val expected = Utility.trim(
      <reply>
        <report>
          <offender><address><addressLine>204 MORNINGSIDE DR</addressLine></address></offender>
          <offender><address><addressLine>204 MORNINGSIDE DR</addressLine></address></offender>
          <offender><address><addressLine>227 N WALLACE AVE</addressLine></address></offender>
          <offender><address><addressLine>327 N WALLACE AVE</addressLine></address></offender>
        </report>
      </reply>
    ) \\ "offender"

    ( actual === expected )(after being streamlined[NodeSeq])
  }

  it should "filter and sort-desc" in {
    val actual = elide( myers, "report:offender+sort-desc=fullname:address:addressLine" ) \\ "offender"
    val expected = Utility.trim(
      <reply>
        <report>
          <offender><address><addressLine>327 N WALLACE AVE</addressLine></address></offender>
          <offender><address><addressLine>227 N WALLACE AVE</addressLine></address></offender>
          <offender><address><addressLine>204 MORNINGSIDE DR</addressLine></address></offender>
          <offender><address><addressLine>204 MORNINGSIDE DR</addressLine></address></offender>
        </report>
      </reply>
    ) \\ "offender"

    ( actual === expected )( after being streamlined[NodeSeq] )
  }

  it should "filter nested" in { 
    val actual = elide( myers, "person:(name:(given,family)),report:offender:offensesByDegree:Other:(description,date)" )
    val expected = Utility.trim(
      <reply>
        <person><name><family>Myers</family><given>Michael</given></name></person>
        <report>
          <offender><offensesByDegree><Other><description>POS ALCOHOL IN STATE PARK</description><date>2004-07-08</date></Other></offensesByDegree></offender>
          <offender><offensesByDegree><Other><description>POSS MARIJUANA >1/2 TO 1 1/2 OZ</description><date>2005-10-09</date></Other></offensesByDegree></offender>
          <offender><offensesByDegree><Other><description>OBT/ATT OBT ALC FALSE DL</description><date>2006-01-13</date></Other></offensesByDegree></offender>
          <offender><offensesByDegree>
            <Other><description>FAILURE TO BURN HEADLAMPS</description><date>2004-06-06</date></Other>
            <Other><description>DRIVE AFTER CONSUMING &lt; 21</description><date>2004-06-06</date></Other>
          </offensesByDegree></offender>
        </report>
      </reply>
    )

    ( actual === expected )( after being streamlined[Elem])
  }
}

object XmlPartialSpec {
  val myers = (
    <reply>
      <inquiryId>381e07f0-453d-11e2-b04c-22000a91952c</inquiryId>
      <orderId>564eaf86-11ae-4d22-b4e8-26bc00484b8c</orderId>
      <person>
        <name>
          <family>Myers</family>
          <given>Michael</given>
          <middle>Charles</middle>
        </name>
        <dateOfBirth>1986-06-29</dateOfBirth>
      </person>
      <report>
        <offender>
          <fullname>MYERS, MICHAEL CHARLES</fullname>
          <dateOfBirth>1986-06-29</dateOfBirth>
          <address>
            <addressLine>204 MORNINGSIDE DR</addressLine>
            <city>CARRBORO</city>
            <regionOrState>NC</regionOrState>
            <zipOrPostalCode>275101251</zipOrPostalCode>
            <county/>
            <country/>
          </address>
          <imageUrl/>
          <offensesByDegree>
            <Other>
              <description>POS ALCOHOL IN STATE PARK</description>
              <degree>Other</degree>
              <date>2004-07-08</date>
            </Other>
          </offensesByDegree>
        </offender>
        <offender>
          <fullname>MYERS, MICHAEL CHUCK</fullname>
          <dateOfBirth>1986-06-29</dateOfBirth>
          <address>
            <addressLine>327 N WALLACE AVE</addressLine>
            <city>WILMINGTON</city>
            <regionOrState>NC</regionOrState>
            <zipOrPostalCode/>
            <county/>
            <country/>
          </address>
          <imageUrl/>
          <offensesByDegree>
            <Other>
              <description>POSS MARIJUANA >1/2 TO 1 1/2 OZ</description>
              <degree>Other</degree>
              <date>2005-10-09</date>
            </Other>
          </offensesByDegree>
        </offender>
        <offender>
          <fullname>MYERS, MICHAEL CHARLIE</fullname>
          <dateOfBirth>1986-06-29</dateOfBirth>
          <address>
            <addressLine>227 N WALLACE AVE</addressLine>
            <city>WILMINGTON</city>
            <regionOrState>NC</regionOrState>
            <zipOrPostalCode>284031251</zipOrPostalCode>
            <county/>
            <country/>
          </address>
          <imageUrl/>
          <offensesByDegree>
            <Other>
              <description>OBT/ATT OBT ALC FALSE DL</description>
              <degree>Other</degree>
              <date>2006-01-13</date>
            </Other>
          </offensesByDegree>
        </offender>
        <offender>
          <fullname>MYERS, MICHAEL C</fullname>
          <dateOfBirth>1986-06-29</dateOfBirth>
          <address>
            <addressLine>204 MORNINGSIDE DR</addressLine>
            <city>CARRBORO</city>
            <regionOrState>NC</regionOrState>
            <zipOrPostalCode/>
            <county/>
            <country/>
          </address>
          <imageUrl/>
          <offensesByDegree>
            <Other>
              <description>FAILURE TO BURN HEADLAMPS</description>
              <degree>Other</degree>
              <date>2004-06-06</date>
            </Other>
            <Other>
              <description>DRIVE AFTER CONSUMING &lt; 21</description>
              <degree>Other</degree>
              <date>2004-06-06</date>
            </Other>
          </offensesByDegree>
        </offender>
      </report>
    </reply>
  )
}
