package omnibus.commons.partial

import org.scalatest._
import org.scalatest.Matchers


class PartialParserSpec extends FlatSpec with Matchers {
  val parser = new PartialParser

  "A Partial Spec Parser" should "produce an empty composite with empty spec" in {
    parser.parse( "" ) shouldBe CompositeCriterion.empty
  }

  it should "produce a prime criterion with single spec" in {
    parser.parse( "foo" ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion() )
    parser.parse( "(foo)" ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion() )
  }

  it should "produce a single prime criterion with property" in {
    parser.parse( "foo+sort=bar" ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion( Map( "sort-asc"->"bar" ) ) )
  }

  it should "produce a composite criterion with single spec" in {
    parser.parse( "foo:(bar)" ) shouldBe CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
    parser.parse( "(foo:(bar))" ) shouldBe CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
  }
  
  it should "produce a composite criterion with multiple subcriteria" in {
    parser.parse( "foo:(bar, zed)" ) shouldBe CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"->PrimeCriterion(), "zed"->PrimeCriterion() ) )
    parser.parse( "foo:bar, zed" ) shouldBe CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"->PrimeCriterion(), "zed"->PrimeCriterion() ) )
    parser.parse( "foo:bar, zed, ted:ma,pa" ) shouldBe CompositeCriterion( Map(), 
      "foo"->CompositeCriterion( Map(), 
        "bar"->PrimeCriterion(), 
        "zed"->PrimeCriterion(), 
        "ted"->CompositeCriterion( Map(), 
          "ma"->PrimeCriterion(), 
          "pa"->PrimeCriterion()
        ) 
      ) 
    )
  }
  
  it should "produce a simple prime list criteria" in {
    parser.parse( "foo,bar" ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
    parser.parse( "(foo,bar)" ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
  }

  it should "ignores whitespace" in {
    parser.parse( "foo, bar" ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
    parser.parse( "   foo ,  bar  " ) shouldBe CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
  }

  it should "produce a simple mix criteria with single spec" in {
    parser.parse( "foo:(bar)" ) shouldBe CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
    parser.parse( "(foo:(bar))" ) shouldBe CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
  }

  it should "product deep nested composite" in {
    parser.parse( "BGC:product:USOneSearch:response:detail:offenders:offender:identity:personal:fullName" ) shouldBe CompositeCriterion( Map(), 
      "BGC"->CompositeCriterion( Map(), 
        "product"->CompositeCriterion( Map(), 
          "USOneSearch"->CompositeCriterion( Map(), 
            "response"->CompositeCriterion( Map(), 
              "detail"->CompositeCriterion( Map(), 
                "offenders"->CompositeCriterion( Map(), 
                  "offender"->CompositeCriterion( Map(), 
                    "identity"->CompositeCriterion( Map(), 
                      "personal"->CompositeCriterion( Map(), 
                        "fullName"->PrimeCriterion()
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  it should "produce complex mixed criteria" in {
    parser.parse( "foo:bar, zed, ted:ma,pa" ) shouldBe CompositeCriterion( Map(), 
      "foo"->CompositeCriterion( Map(), 
        "bar"->PrimeCriterion(), 
        "zed"->PrimeCriterion(), 
        "ted"->CompositeCriterion( Map(), 
          "ma"->PrimeCriterion(), 
          "pa"->PrimeCriterion()
        )
      ) 
    )

    parser.parse( "augur, foo:(bar, zed, ted:ma,pa), seahawks" ) shouldBe CompositeCriterion( Map(), 
      "augur"->PrimeCriterion(),
      "foo"->CompositeCriterion( Map(), 
        "bar"->PrimeCriterion(), 
        "zed"->PrimeCriterion(), 
        "ted"->CompositeCriterion( Map(), 
          "ma"->PrimeCriterion(), 
          "pa"->PrimeCriterion()
        )
      ),
      "seahawks"->PrimeCriterion()
    )

    parser.parse( "foo:(bar, zed, ted:ma,pa), augur, seahawks" ) shouldBe CompositeCriterion( Map(), 
      "augur"->PrimeCriterion(),
      "foo"->CompositeCriterion( Map(), 
        "bar"->PrimeCriterion(), 
        "zed"->PrimeCriterion(), 
        "ted"->CompositeCriterion( Map(), 
          "ma"->PrimeCriterion(), 
          "pa"->PrimeCriterion()
        )
      ),
      "seahawks"->PrimeCriterion()
    )

    parser.parse( "foo:bar, zed, ted:(ma,pa), augur, seahawks" ) shouldBe CompositeCriterion( Map(), 
      "foo"->CompositeCriterion( Map(), 
        "bar"->PrimeCriterion(), 
        "zed"->PrimeCriterion(), 
        "ted"->CompositeCriterion( Map(), 
          "ma"->PrimeCriterion(), 
          "pa"->PrimeCriterion()
        ),
      "augur"->PrimeCriterion(),
      "seahawks"->PrimeCriterion()
      )
    )
  }

  it should "produce mix of prime and multiple composite" in {
    parser.parse( "inquiryId,orderId,person:(name:(given,family)),report:offender:address:addressLine" ) shouldBe CompositeCriterion( Map(), 
      "inquiryId" -> PrimeCriterion(),
      "orderId" -> PrimeCriterion(),
      "person" -> CompositeCriterion( Map(), 
        "name" -> CompositeCriterion( Map(), 
          "given" -> PrimeCriterion(),
          "family" -> PrimeCriterion()
        )
      ),
      "report" -> CompositeCriterion( Map(), 
        "offender" -> CompositeCriterion( Map(), 
          "address" -> CompositeCriterion( Map(), 
            "addressLine" -> PrimeCriterion()
          )
        )
      )
    )

    parser.parse( "person:(name:(given,family)),report:offender:offensesByDegree:Other:(description,date)" ) shouldBe CompositeCriterion( Map(), 
      "person" -> CompositeCriterion( Map(), 
        "name" -> CompositeCriterion( Map(), 
          "given" -> PrimeCriterion(),
          "family" -> PrimeCriterion()
        )
      ),
      "report" -> CompositeCriterion( Map(), 
        "offender" -> CompositeCriterion( Map(), 
          "offensesByDegree" -> CompositeCriterion( Map(), 
            "Other" -> CompositeCriterion( Map(), 
              "description" -> PrimeCriterion(),
              "date" -> PrimeCriterion()
            )
          )
        )
      )
    )
  }
}
