package peds.commons.collection

import org.scalatest._
import org.scalatest.Matchers
import com.typesafe.scalalogging.LazyLogging


/**
 * There's a lot of math involved in the Jaro-Winkler
 * Distance calculator.  My implementation came straight
 * from the formulas on the Wikipedia article for the
 * calculation (http://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance).
 * Many of the test samples (martha, marhta) are from
 * the website, including the values of each step in 
 * the calculation (matching window, jaro distance, matches, transposes, etc).
 * If these values are wrong, someone needs to update Wikipedia,
 * because I'm use their results to validate my implementation!
 */
class StringWithSimilaritySpec extends FlatSpec with Matchers with LazyLogging {
	"A string with similarity" should "Matching window returns valid distance" in {
		val one = "martha"
		val two = "marhta"
		StringWithSimilarity.matchingWindow( one, two ) should equal( 2 )
		StringWithSimilarity.matchingWindow( two, one ) should equal( 2 )
		
		val three = "mart"
		val four = "marha"
		StringWithSimilarity.matchingWindow( three, four ) should equal( 1 )
		StringWithSimilarity.matchingWindow( four, three ) should equal( 1 )
	}

	it should "Winkler common prefix returns the correct prefix size" in {
		val str1OfShouldReturn2 = "joe"
		val str2OfShouldReturn2 = "joseph"
		StringWithSimilarity.winklerCommonPrefix( str1OfShouldReturn2, str2OfShouldReturn2 ) should equal( 2 )
		StringWithSimilarity.winklerCommonPrefix( str2OfShouldReturn2, str1OfShouldReturn2 ) should equal( 2 )
		
		val str1OfShouldReturn4 = "abcdfg"
		val str2OfShouldReturn4 = "abcdef"
		StringWithSimilarity.winklerCommonPrefix( str1OfShouldReturn4, str2OfShouldReturn4 ) should equal( 4 )
		StringWithSimilarity.winklerCommonPrefix( str2OfShouldReturn4, str1OfShouldReturn4 ) should equal( 4 )
		
		val str1OfShouldReturn4ButCouldMore = "abcdef"
		val str2OfShouldReturn4ButCouldMore = "abcdef"
		StringWithSimilarity.winklerCommonPrefix( str1OfShouldReturn4ButCouldMore, str2OfShouldReturn4ButCouldMore ) should equal( 4 )
		StringWithSimilarity.winklerCommonPrefix( str2OfShouldReturn4ButCouldMore, str1OfShouldReturn4ButCouldMore ) should equal( 4 )

		val str1OfShouldReturn3 = "martha"
		val str2OfShouldReturn3 = "marhta"
		StringWithSimilarity.winklerCommonPrefix( str1OfShouldReturn3, str2OfShouldReturn3 ) should equal( 3 )
		StringWithSimilarity.winklerCommonPrefix( str2OfShouldReturn3, str1OfShouldReturn3 ) should equal( 3 )
	}

	it should "Determine matches and transposes returns the correct values" in {
		val one = "martha"
		val two = "marhta"
		val mr = StringWithSimilarity.determineMatchesAndTransposes( one, two )
		mr should be( StringWithSimilarity.MatchResults( numberOfMatches = 6, numberOfTransposes = 1 ) )
	}

	it should "Jaro distance returns the correct value" in {
		val one = "martha"
		val two = "marhta"
		
		val matchResults = StringWithSimilarity.determineMatchesAndTransposes( one, two )
		
		val jaroDistance = StringWithSimilarity.jaroDistance(
			matchResults.numberOfMatches, 
			matchResults.numberOfTransposes, 
			one.length,
			two.length
		)
		
		jaroDistance should be ( 0.944 +- 0.05 )
	}

	it should "Jaro winkler similarity returns the correct value" in {
		val one = "martha"
		val two = "marhta"
		
		val jaroWinklerSimilarity = one similarityWith two
		jaroWinklerSimilarity should be ( 0.961 +- 0.05 )
	}
}
