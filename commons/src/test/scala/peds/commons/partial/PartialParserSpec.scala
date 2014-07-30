package peds.commons.partial

import org.specs2._
import com.typesafe.scalalogging.LazyLogging


class PartialParserSpec() extends mutable.Specification with LazyLogging {
  "An Partial Spec Parser" should {
    val parser = new PartialParser

    "produce an empty composite with empty spec" in {
      parser.parse( "" ) must_== CompositeCriterion.empty
    }

    "produce a prime criterion with single spec" in {
      parser.parse( "foo" ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion() )
      parser.parse( "(foo)" ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion() )
    }

    "produce a single prime criterion with property" in {
      parser.parse( "foo+sort=bar" ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion( Map( "sort-asc"->"bar" ) ) )
    }

    "produce a composite criterion with single spec" in {
      parser.parse( "foo:(bar)" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
      parser.parse( "(foo:(bar))" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
    }
    
    "produce a composite criterion with multiple subcriteria" in {
      parser.parse( "foo:(bar, zed)" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"->PrimeCriterion(), "zed"->PrimeCriterion() ) )
      parser.parse( "foo:bar, zed" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"->PrimeCriterion(), "zed"->PrimeCriterion() ) )
      parser.parse( "foo:bar, zed, ted:ma,pa" ) must_== CompositeCriterion( Map(), 
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
    
    "produce a simple prime list criteria" in {
      parser.parse( "foo,bar" ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
      parser.parse( "(foo,bar)" ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
    }

    "ignores whitespace" in {
      parser.parse( "foo, bar" ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
      parser.parse( "   foo ,  bar  " ) must_== CompositeCriterion( Map(), "foo"->PrimeCriterion(), "bar"-> PrimeCriterion() )
    }

    "produce a simple mix criteria with single spec" in {
      parser.parse( "foo:(bar)" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
      parser.parse( "(foo:(bar))" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
    }

    "produce a simple mix criteria with single spec" in {
      parser.parse( "foo:(bar)" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
      parser.parse( "(foo:(bar))" ) must_== CompositeCriterion( Map(), "foo"->CompositeCriterion( Map(), "bar"-> PrimeCriterion() ) )
    }

    "product deep nested composite" in {
      parser.parse( "BGC:product:USOneSearch:response:detail:offenders:offender:identity:personal:fullName" ) must_== CompositeCriterion( Map(), 
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

    "produce complex mixed criteria" in {
      parser.parse( "foo:bar, zed, ted:ma,pa" ) must_== CompositeCriterion( Map(), 
        "foo"->CompositeCriterion( Map(), 
          "bar"->PrimeCriterion(), 
          "zed"->PrimeCriterion(), 
          "ted"->CompositeCriterion( Map(), 
            "ma"->PrimeCriterion(), 
            "pa"->PrimeCriterion()
          )
        ) 
      )

      parser.parse( "augur, foo:(bar, zed, ted:ma,pa), seahawks" ) must_== CompositeCriterion( Map(), 
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

      parser.parse( "foo:(bar, zed, ted:ma,pa), augur, seahawks" ) must_== CompositeCriterion( Map(), 
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

      parser.parse( "foo:bar, zed, ted:(ma,pa), augur, seahawks" ) must_== CompositeCriterion( Map(), 
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

    "produce mix of prime and multiple composite" in {
      parser.parse( "inquiryId,orderId,person:(name:(given,family)),report:offender:address:addressLine" ) must_== CompositeCriterion( Map(), 
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

      parser.parse( "person:(name:(given,family)),report:offender:offensesByDegree:Other:(description,date)" ) must_== CompositeCriterion( Map(), 
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
}
