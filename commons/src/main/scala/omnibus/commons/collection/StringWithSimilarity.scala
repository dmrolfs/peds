package omnibus.commons.collection

/**
  * Find the similarity of two strings using the Jaro-Winkler
  * distance algorithm.
  * http://en.wikipedia.org/wiki/Jaro%E2%80%93Winkler_distance
  */
//todo: make into implicit value class
@deprecated( "use rockymadden/stringmetric library instead", "0.61" )
class StringWithSimilarity( lhs: String ) {
  import StringWithSimilarity._

  /**
	 * Calculate the Jaro-Winkler Similarity of two strings
	 * @param lhs First String
	 * @param rhs Second String
	 * @return Jaro-Winkler similarity value
	 */
  def similarityWith( rhs: String ): Double = {
    val matchResults = determineMatchesAndTransposes( lhs, rhs )
    // trace( "matchResults="+matchResults)
    val jd: Double = jaroDistance(
      matchResults.numberOfMatches,
      matchResults.numberOfTransposes,
      lhs.length,
      rhs.length
    )
    // trace( "jaro distance = " + jd )

    val wcp: Int = winklerCommonPrefix( lhs, rhs )
    // trace( "winkler Common Prefix = " + wcp )

    //Find the Jaro-Winkler Distance = Jd + (l * p * ( 1 - Jd));
    jd + (wcp * prefixScalingFactor) * (1 - jd)
  }
}

object StringWithSimilarity {
  val trace = omnibus.commons.log.Trace[StringWithSimilarity]
  //Bonus weighting for string starting with the same characters
  //(e.g.: prefix scaling factor)
  val prefixScalingFactor: Double = 0.1

  /**
	 * Instead of making two seperate functions for matching
	 * and transposes (which would be terrible since you
	 * find the transpose while doing matching), I created this
	 * little class to hold the results of both.
	 */
  case class MatchResults( val numberOfMatches: Int = 0, val numberOfTransposes: Int = 0 )

  /**
	 * Find the all of the matching and transposed characters in
	 * two strings
	 * @param lhs First String
	 * @param rhs Second String
	 * @return A Match Result with both the number of matches and
	 * number of transposed characters
	 */
  def determineMatchesAndTransposes( lhs: String, rhs: String ): MatchResults = {
    val window = matchingWindow( lhs, rhs )
    // trace("window="+window)

    val ( shortest, longest ) = {
      if (lhs.length <= rhs.length) ( lhs.toCharArray, rhs.toCharArray )
      else ( rhs.toCharArray, lhs.toCharArray )
    }
    // trace("(shortest, longest)="+(shortest.mkString, longest.mkString))

    var matchedOutOfPosition = 0
    var numberOfMatches = 0

    for (i <- 0 until shortest.length) {
      if (shortest( i ) == longest( i )) numberOfMatches += 1
      else {
        //Set the boundary of how far back we can look at the longest string
        //given the string's size and the window size
        val backwardBoundary = if ((i - window) < 0) 0 else (i - window)
        // trace("backwardBoundary="+backwardBoundary)

        //Set the boundary of how far we can look ahead at the longest string
        //given the string's size and the window size
        val forwardBoundary =
          if ((i + window) > (longest.length - 1)) (longest.length - 1) else (i + window)
        // trace("forwardBoundary="+forwardBoundary)

        //Starting at the back-most point, and moving to the forward-most point of the
        //longest string, iterate looking for matches of our current character on the shortest  string

        var matchFound = false
        for {
          b <- backwardBoundary to forwardBoundary
          if !matchFound && (longest( b ) == shortest( i ))
        } {
          // trace("b="+b)
          numberOfMatches += 1
          matchedOutOfPosition += 1
          // trace("numberOfMatches="+numberOfMatches)
          // trace("matchedOutOfPosition="+matchedOutOfPosition)
          matchFound = true
        }
      }
    }

    //Determine the number of transposes by halving the number of out-of-position matches.
    //This works because if we had two strings: "martha" and "marhta", the 'th' and 'ht' are
    //transposed, but would be counted twice in the process above.
    val numberOfTransposes = matchedOutOfPosition / 2
    MatchResults( numberOfMatches = numberOfMatches, numberOfTransposes = numberOfTransposes )
  }

  /**
	 * Determine the maximum window size to use when looking for matches.
	 * The window is basically a little less than the half the longest
	 * string's length.
	 * Equation: [ Max(A, B) / 2 ] -1
	 * @param lhs First String
	 * @param rhs Second String
	 * @return Max window size
	 */
  def matchingWindow( lhs: String, rhs: String ): Int = ((lhs.length max rhs.length) / 2) - 1

  /**
	 * Determine the Jaro Distance.  Winkler stole some of Jaro's
	 * thunder by adding some more math onto his algorithm and getting
	 * equal parts credit!  However, we still need Jaro's Distance
	 * to get the Jaro-Winkler Distance.
	 * Equation: 1/3 * ((|A| / m) + (|B| / m) + ((m - t) / m))
	 * Where: |A| = length of first string
	 *        |B| = length of second string
	 *         m  = number of matches
	 *         t  = number of transposes
	 * @param numMatches Number of matches
	 * @param numTransposes Number of transposes
	 * @param lhsLength Length of String one
	 * @param rhsLength Length of String two
	 * @return Jaro Distance
	 */
  def jaroDistance(
    numMatches: Int,
    numTransposes: Int,
    lhsLength: Int,
    rhsLength: Int
  ): Double = {
    // trace( "numMatches="+numMatches )
    // trace( "numTransposes="+numTransposes )
    // trace( "lhsLength="+lhsLength )
    // trace( "rhsLength="+rhsLength )
    val third: Double = 1.0 / 3.0

    // (|A| / m)
    val lhsNorm: Double = numMatches.toDouble / lhsLength.toDouble
    // (|B| / m)
    val rhsNorm: Double = numMatches.toDouble / rhsLength.toDouble
    // ((m - t) / m)
    val matchTransNorm: Double = (numMatches - numTransposes).toDouble / numMatches.toDouble
    // 1/3 * ((|A| / m) + (|B| / m) + ((m - t) / m))
    third * (lhsNorm + rhsNorm + matchTransNorm)
  }

  /**
	 * Find the Winkler Common Prefix of two strings.  In Layman's terms,
	 * find the number of character that match at the beginning of the
	 * two strings, with a maximum of 4.
	 * @param lhs First string
	 * @param rhs Second string
	 * @return Integer between 0 and 4 representing the number of
	 * matching characters at the beginning of both strings.
	 */
  def winklerCommonPrefix( lhs: String, rhs: String ): Int = {
    val boundary = lhs.length min rhs.length
    var result = 0
    var continue = true
    for (i <- 0 until boundary if continue) {
      if (lhs( i ) == rhs( i )) result += 1
      else continue = false
      if (result == 4) continue = false
    }

    result
  }
}
