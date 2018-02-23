package omnibus.commons.partial

import org.scalatest._
import org.scalatest.Matchers
import org.json4s.jackson.JsonMethods._


class JsonPartialSpec extends FlatSpec with Matchers {
  import JsonElisionSpec._
  import JsonReducable._


  "An elided JSON API" should "filter simple list" in {
    elide( myers, "inquiryId, orderId" ) shouldBe parse(
      """{ 
        "inquiryId": "381e07f0-453d-11e2-b04c-22000a91952c", 
        "orderId": "564eaf86-11ae-4d22-b4e8-26bc00484b8c" 
      }"""
    )
  }

  it should "filter simple composite list" in {
    elide( myers, "person:name:(given,family)" ) shouldBe parse(
      """{
        "person": {
          "name": {
            "family": "Myers",
            "given": "Michael"
          }
        }
      }"""
    )
  }

  it should "filter simple mix of prime and composite list" in {
    elide( myers, "inquiryId,orderId,person:name:(given,family)" ) shouldBe parse(
      """{
        "inquiryId": "381e07f0-453d-11e2-b04c-22000a91952c",
        "orderId": "564eaf86-11ae-4d22-b4e8-26bc00484b8c",
        "person": {
          "name": {
            "family": "Myers",
            "given": "Michael"
          }
        }
      }"""
    )
  }

  it should "filter simple mix of prime and two simple composite list" in {
    // trace( "myers data = "+myers )
    elide( myers, "inquiryId,orderId,person:(name:(given,family)),report:offenders:address:addressLines" ) shouldBe parse(
      """{
        "inquiryId": "381e07f0-453d-11e2-b04c-22000a91952c",
        "orderId": "564eaf86-11ae-4d22-b4e8-26bc00484b8c",
        "person": { "name": { "family": "Myers", "given": "Michael" } },
        "report": {
          "offenders": [
            { "address": { "addressLines": ["204 MORNINGSIDE DR"] } },
            { "address": { "addressLines": ["327 N WALLACE AVE"] } },
            { "address": { "addressLines": ["227 N WALLACE AVE"] } },
            { "address": { "addressLines": ["204 MORNINGSIDE DR"] } }
          ]}
      }"""
    )
  }

  it should "filter and sort" in {
    val actual = elide( myers, "report:offenders+sort=fullname:address:addressLines" ) 
    val expected = parse(
      """{
        "report" : {
          "offenders": [{
            "address": {
              "addressLines": ["204 MORNINGSIDE DR"]
            }
          }, {
            "address": {
              "addressLines": ["204 MORNINGSIDE DR"]
            }
          }, {
            "address": {
              "addressLines": ["227 N WALLACE AVE"]
            }
          }, {
            "address": {
              "addressLines": ["327 N WALLACE AVE"]
            }
          }]
        }
      }"""
    )

    actual shouldBe expected
  }

  it should "filter and sort-desc" in {
    elide( myers, "report:offenders+sort-desc=fullname:address:addressLines" ) shouldBe parse(
      """{
        "report" : {
          "offenders": [{
            "address": {
              "addressLines": ["327 N WALLACE AVE"]
            }
          }, {
            "address": {
              "addressLines": ["227 N WALLACE AVE"]
            }
          }, {
            "address": {
              "addressLines": ["204 MORNINGSIDE DR"]
            }
          }, {
            "address": {
              "addressLines": ["204 MORNINGSIDE DR"]
            }
          }]
        }
      }"""
    )
  }

  it should "simple filter and sort" in {
    val data = parse(
      """{
        "bars": [{
          "alpha": "c",
          "foo": "3"
        }, {
          "foo": "2",
          "alpha": "b"
        }, {
          "alpha": "a",
          "foo": "1"
        }],
        "zed": "do not include"
      }"""
    )

    elide( data, "bars" ) shouldBe parse(
      """{
        "bars": [{
          "alpha": "c",
          "foo": "3"
        }, {
          "foo": "2",
          "alpha": "b"
        }, {
          "alpha": "a",
          "foo": "1"
        }]
      }"""
    )

    ( elide( data, "bars+sort=foo" ) \ "bars" ) shouldBe parse( 
      """{
        "bars": [{
          "alpha": "a",
          "foo": "1"
        }, {
          "foo": "2",
          "alpha": "b"
        }, {
          "alpha": "c",
          "foo": "3"
        }]
      }"""
    ) \ "bars"

    ( elide( data, "bars+sort=foo:alpha" ) \ "bars" ) shouldBe parse( 
      """{ "bars": [{ "alpha": "a" }, { "alpha": "b" }, { "alpha": "c" }] }"""
    ) \ "bars"
  }

  it should "simple filter and sort-desc" in {
    val data = parse(
      """{
        "bars": [{
          "alpha": "c",
          "foo": "3"
        }, {
          "foo": "2",
          "alpha": "b"
        }, {
          "alpha": "a",
          "foo": "1"
        }],
        "zed": "do not include"
      }"""
    )

    elide( data, "bars" ) shouldBe parse(
      """{
        "bars": [{
          "alpha": "c",
          "foo": "3"
        }, {
          "foo": "2",
          "alpha": "b"
        }, {
          "alpha": "a",
          "foo": "1"
        }]
      }"""
    )

    ( elide( data, "bars+sort-desc=foo" ) \ "bars" ) shouldBe parse(
      """{
        "bars": [{
          "alpha": "c",
          "foo": "3"
        }, {
          "foo": "2",
          "alpha": "b"
        }, {
          "alpha": "a",
          "foo": "1"
        }]
      }"""
    ) \ "bars"

    ( elide( data, "bars+sort-desc=foo:alpha" ) \ "bars" ) shouldBe parse(
      """{
        "bars": [{
          "alpha": "c"
        }, {
          "alpha": "b"
        }, {
          "alpha": "a"
        }]
      }"""
    ) \ "bars"
  }

  it should "filter nested" in {
    elide( myers, "person:(name:(given,family)),report:offenders:offensesByDegree:Other:(description,date)" ) shouldBe parse(
      """{
        "person": { "name": { "family": "Myers", "given": "Michael" } },
        "report": {
          "offenders": [
            {
              "offensesByDegree": [{
                "Other": [{
                  "description": "POS ALCOHOL IN STATE PARK",
                  "date": "2004-07-08"
                }]
              }]
            },
            {
              "offensesByDegree": [{
                "Other": [{
                  "description": "POSS MARIJUANA >1/2 TO 1 1/2 OZ",
                  "date": "2005-10-09"
                }]
              }]
            },
            {
              "offensesByDegree": [{
                "Other": [{
                  "description": "OBT/ATT OBT ALC FALSE DL",
                  "date": "2006-01-13"
                }]
              }]
            },
            {
              "offensesByDegree": [{
                "Other": [{
                  "description": "FAILURE TO BURN HEADLAMPS",
                  "date": "2004-06-06"
                }, {
                  "description": "DRIVE AFTER CONSUMING < 21",
                  "date": "2004-06-06"
                }]
              }]
            }
          ]
        }
      }"""
    )
  }
}

object JsonElisionSpec {
  val source = 
"""{
  "inquiryId": "381e07f0-453d-11e2-b04c-22000a91952c",
  "orderId": "564eaf86-11ae-4d22-b4e8-26bc00484b8c",
  "person": {
    "name": {
      "family": "Myers",
      "given": "Michael",
      "middle": "Charles"
    },
    "dateOfBirth": "1986-06-29"
  },
  "report": {
    "offenders": [{
      "fullname": "MYERS, MICHAEL CHARLES",
      "dateOfBirth": "1986-06-29",
      "address": {
        "addressLines": ["204 MORNINGSIDE DR"],
        "city": "CARRBORO",
        "regionOrState": "NC",
        "zipOrPostalCode": "275101251",
        "county": "",
        "country": ""
      },
      "imageUrl": "",
      "offensesByDegree": [{
        "Other": [{
          "description": "POS ALCOHOL IN STATE PARK",
          "degree": "Other",
          "date": "2004-07-08"
        }]
      }]
    }, {
      "fullname": "MYERS, MICHAEL CHUCK",
      "dateOfBirth": "1986-06-29",
      "address": {
        "addressLines": ["327 N WALLACE AVE"],
        "city": "WILMINGTON",
        "regionOrState": "NC",
        "zipOrPostalCode": "",
        "county": "",
        "country": ""
      },
      "imageUrl": "",
      "offensesByDegree": [{
        "Other": [{
          "description": "POSS MARIJUANA >1/2 TO 1 1/2 OZ",
          "degree": "Other",
          "date": "2005-10-09"
        }]
      }]
    }, {
      "fullname": "MYERS, MICHAEL CHARLIE",
      "dateOfBirth": "1986-06-29",
      "address": {
        "addressLines": ["227 N WALLACE AVE"],
        "city": "WILMINGTON",
        "regionOrState": "NC",
        "zipOrPostalCode": "284031251",
        "county": "",
        "country": ""
      },
      "imageUrl": "",
      "offensesByDegree": [{
        "Other": [{
          "description": "OBT/ATT OBT ALC FALSE DL",
          "degree": "Other",
          "date": "2006-01-13"
        }]
      }]
    }, {
      "fullname": "MYERS, MICHAEL C",
      "dateOfBirth": "1986-06-29",
      "address": {
        "addressLines": ["204 MORNINGSIDE DR"],
        "city": "CARRBORO",
        "regionOrState": "NC",
        "zipOrPostalCode": "",
        "county": "",
        "country": ""
      },
      "imageUrl": "",
      "offensesByDegree": [{
        "Other": [{
          "description": "FAILURE TO BURN HEADLAMPS",
          "degree": "Other",
          "date": "2004-06-06"
        }, {
          "description": "DRIVE AFTER CONSUMING < 21",
          "degree": "Other",
          "date": "2004-06-06"
        }]
      }]
    }]
  }
}"""

  val myers = parse( source )

}