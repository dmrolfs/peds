package peds.commons.collection

import java.io.{InputStream, OutputStream}
import collection.immutable.BitSet
import com.typesafe.scalalogging.LazyLogging


@deprecated( "use fast bloomfilter", "20170112" )
class BloomFilter[T] private ( 
  val size: Int, 
  val width: Int,
  val k: Int, 
  private val hashable: Hashable, 
  /*private*/ val buckets: BitSet
)( implicit val manifest: Manifest[T] ) 
extends ( (T) => Boolean ) 
with Equals 
with Serializable 
with LazyLogging {

  import BloomFilter._
  
  require( size >= 0 )
  require( width > 0 )
  require( k > 0 )

  def this( width: Int, numHashes: Int, hashable: Hashable )( implicit manifest: Manifest[T] ) = {
    this( 0, width, numHashes, hashable, BitSet() )( manifest )
  }
    
  def this( width: Int, numHashes: Int )( implicit manifest: Manifest[T] ) = {
    this( width, numHashes, MurmurHashable )( manifest )
  }
  
  private val hashFn = hashable.hashes( k )( width ) _
  
  /**
   * <p>A value between 0 and 1 which estimates the accuracy of the bloom filter.
   * This estimate is precisely the inverse of the probability function for
   * a bloom filter of a given capacity, width and number of hash functions.  The
   * probability function given by the following expression in LaTeX math syntax:</p>
   * 
   * <p><code>(1 - e^{-kn/m})^k</code> where <i>k</i> is the number of hash functions,
   * <i>n</i> is the number of elements in the bloom filter and <i>m</i> is the
   * width.</p>
   * 
   * <p>It is important to remember that this is only an estimate of the accuracy.
   * Likewise, it assumes perfectly ideal hash functions, thus it is somewhat
   * more optimistic than the reality of the implementation.</p>
   */
  lazy val accuracy = calculateAccuracy( k, size, width )
  
  /**
   * Returns the optimal value of <i>k</i> for a bloom filter with the current
   * properties (width and capacity).  Useful in reducing false-positives on sets
   * with a limited range in capacity.
   */
  lazy val optimalK = calculateOptimalK( size, width )
  
  def +( value: T ) = new BloomFilter[T]( size + 1, width, k, hashable, add(buckets)(value) )
  
  protected def add( buckets: BitSet )( value: T ) = hashFn( value ).foldLeft( buckets ) { _ + _ }

  def has_?( value: T ): Boolean = {
    for ( i <- hashFn(value) ) {
      if ( !buckets.contains(i) ) return false
    }
    
    return true
  }
  
  def apply( value: T ) = has_?( value )
  

  def ++( col: Traversable[T] ): BloomFilter[T] = {
    var length = 0
    val newbuckets = col.foldLeft( buckets ) { (c, e) =>
      length += 1
      add( c )( e )
    }
    
    new BloomFilter( size + length, width, k, hashable, newbuckets )
  }
  
  /**
   * Computes the union of two bloom filters and returns the result.  Note that
   * this operation is only defined for filters of the same width.  Filters which
   * have a different value of <i>k</i> (different number of hash functions) can
   * be unioned, but the result will have a higher probability of false positives
   * than either of the operands.  The <i>k</i> value of the resulting filter is
   * computed to be the minimum <i>k</i> of the two operands.  The <i>capacity</i> of
   * the resulting filter is precisely the combined capacity of the operands.  This
   * of course means that for sets with intersecting items the capacity will be
   * slightly large.
   */
  def ++( set: BloomFilter[T] ) = {
    if ( set.width != width ) {
      throw new IllegalArgumentException( "Bloom filter union is only defined for " +
          "sets of the same width (" + set.width + " != " + width + ")" )
    }
    
    if ( set.k != k ) {
      throw new IllegalArgumentException( "Bloom filter union is only defined for " +
          "sets of the same k (" + set.k + " != " + k + ")" )
    }
    
    val newbuckets = buckets | set.buckets
    // min guarantees no false negatives
    new BloomFilter[T](size + set.size, width, k, hashable, newbuckets ) 
  }

//dmr: serialize using kyro? protobuf? options?  
  // def store( os: OutputStream ) {
  //   os.write( convertToBytes(capacity) )
  //   os.write( convertToBytes(k) )
  //   os.write( convertToBytes(buckets.size) )
  // 
  //   var num = 0
  //   var card = 0
  //   for ( b <- 0 to buckets.last ) {
  //     num = ( num << 1 ) | ( if (buckets(b)) 1 else 0 )
  //     card += 1
  //   
  //     if ( card == 8 ) {
  //       os.write( num )
  //     
  //       num = 0
  //       card = 0
  //     }
  //   }
  // 
  //   if ( card != 0 ) os.write( num )
  // }
  

  override def equals( other: Any ) = other match {
    case that: BloomFilter[T] => {
logger.info( s"this == other: manifest = ${this.manifest == other.asInstanceOf[BloomFilter[T]].manifest}" )
logger.info( s"this == other: size = ${this.size == other.asInstanceOf[BloomFilter[T]].size}" )
logger.info( s"this == other: width = ${this.width == other.asInstanceOf[BloomFilter[T]].width}" )
logger.info( s"this == other: k = ${this.k == other.asInstanceOf[BloomFilter[T]].k}" )
logger.info( s"this == other: hashable = ${this.hashable == other.asInstanceOf[BloomFilter[T]].hashable}" )
logger.info( s"this == other: buckets = ${this.buckets == other.asInstanceOf[BloomFilter[T]].buckets}" )
logger.info( s"this | other: buckets.size = ${this.buckets.size} | ${other.asInstanceOf[BloomFilter[T]].buckets.size}" )

      if ( this eq that ) true
      else {
        val result = (
          ( that.## == this.## ) &&
          ( that canEqual this ) && 
          ( that.manifest == manifest ) &&
          ( that.size == size ) &&
          ( that.width == width ) &&
          ( that.k == k ) &&
          ( that.hashable == hashable ) &&
          ( that.buckets.size == buckets.size )
        )
logger.info( s"first result = $result" )
        ( 0 until buckets.size ).foldLeft( result ) { (acc, i) =>
          acc && ( buckets(i) == that.buckets(i) )
        }
      }
    }
    
    case _ => false
  }
  
  override def canEqual( that: Any ) = that.isInstanceOf[BloomFilter[T]]
  
  override def hashCode = {
    ( 0 until width ).foldLeft( manifest.## ^ size ^ width ^ k ) { (acc, i) =>
      acc ^ ( if ( buckets(i) ) i else 0 )
    }
  }
}


object BloomFilter extends LazyLogging {
  def apply[T]()( implicit manifest: Manifest[T] ): BloomFilter[T] = apply[T]( 0.05, 100 )( manifest )
  
  /**
   * @return The smallest BloomFilter that can provide the given false
   *         positive probability rate for the given number of elements.
   *
   *         Asserts that the given probability can be satisfied using this
   *         filter.
   */
  def apply[T]( maxFalsePosProbability: Double, size: Int )( implicit manifest: Manifest[T] ): BloomFilter[T] = {
    make( maxFalsePosProbability, size, MurmurHashable )
  }
  
  def apply[T]( size: Int, bucketsPerElement: Double, numHashes: Int )( implicit manifest: Manifest[T] ) = {
    new BloomFilter[T]( (size * bucketsPerElement).toInt, numHashes )( manifest )
  }
  
  def apply[T]( 
    size: Int, 
    targetBucketsPerElem: Double, 
    hashable: Hashable 
  )( implicit manifest: Manifest[T] ): BloomFilter[T] = {
    val maxBuckets: Int = maxBucketsPerElement( size ) max 1
    val bucketsPerElement = targetBucketsPerElem min maxBuckets

    if ( bucketsPerElement < targetBucketsPerElem ) {
      logger.warn( 
        "Cannot provide an optimal BloomFilter for %d elements (%d/%d buckets per element).".format(
          size, 
          bucketsPerElement, 
          targetBucketsPerElem
        )
      )
    }
    
    val spec = BloomSpecification( bucketsPerElement )
    make( size, spec.bucketsPerElement, spec.numHashes, hashable )
  }

  private[collection] def make[T]( 
    maxFalsePosProbability: Double, 
    size: Int, 
    hashable: Hashable
  )( implicit manifest: Manifest[T] ): BloomFilter[T] = {
    require( maxFalsePosProbability <= 1.0 )
    val bucketsPerElement: Double = maxBucketsPerElement( size )
    val spec = BloomSpecification( bucketsPerElement, maxFalsePosProbability )
    make( size, spec.bucketsPerElement, spec.numHashes, hashable )
  }
  
  private[collection] def make[T]( 
    numElements: Int, 
    bucketsPerElem: Double, 
    numHashes: Int, 
    hashable: Hashable
  )( implicit manifest: Manifest[T] ): BloomFilter[T] = {
    new BloomFilter[T]( 
      0, 
      (numElements * bucketsPerElem).intValue,
      numHashes, 
      hashable, 
      BitSet()
    )( manifest )
  } 

  // @deprecated
  // def apply[T]( values: T* )( implicit manifest: Manifest[T] ): BloomFilter[T] = {
  //   values.foldLeft( new BloomFilter[T](200, 4)(manifest) ) { _ + _ }
  // }
  // 
  // @deprecated
  // def apply[T]( width: Int, k: Int )( implicit manifest: Manifest[T] ) = new BloomFilter[T]( width, k )( manifest )

  /**
   * <p>A value between 0 and 1 which estimates the accuracy of the bloom filter.
   * This estimate is precisely the inverse of the probability function for
   * a bloom filter of a given capacity, width and number of hash functions.  The
   * probability function given by the following expression in LaTeX math syntax:</p>
   * 
   * <p><code>(1 - e^{-kn/m})^k</code> where <i>k</i> is the number of hash functions,
   * <i>n</i> is the number of elements in the bloom filter and <i>m</i> is the
   * width.</p>
   * 
   * <p>It is important to remember that this is only an estimate of the accuracy.
   * Likewise, it assumes perfectly ideal hash functions, thus it is somewhat
   * more optimistic than the reality of the implementation.</p>
   */
  def calculateAccuracy( numHashes: Int, expectedSize: Int, width: Int ): Double = calculateAccuracy( 
    numHashes, 
    ( width.doubleValue / expectedSize.doubleValue ) 
  )

  def calculateAccuracy( numHashes: Int, bucketsPerElement: Double ): Double = {
    require( bucketsPerElement > 0.0 )
    require( numHashes > 0 )
    val exp = ( numHashes: Double ) / bucketsPerElement
    val probability = math.pow( 1 - math.exp(-exp), numHashes )
    1d - probability
  }


  /**
   * Returns the optimal value of <i>k</i> for a bloom filter with the current
   * properties (width and capacity).  Useful in reducing false-positives on sets
   * with a limited range in capacity.
   */
  def calculateOptimalK( expectedSize: Int, width: Int ): Int = calculateOptimalK( 
    width.doubleValue / expectedSize.doubleValue 
  )


  def calculateOptimalK( bucketsPerElement: Double ): Int = {
    val ln2 = math.log( 2 )
    (ln2 * bucketsPerElement).intValue max 1
  }

  /**
   * Calculates the maximum number of buckets per element that this implementation
   * can support.  Crucially, it will lower the bucket count if necessary to meet
   * BitSet's size restrictions.
   */
  def maxBucketsPerElement( numElements: Long ): Int = {
    val elements = numElements max 1
    val v: Double = ( Int.MaxValue - BloomSpecification.EXCESS ) / elements.doubleValue
    if ( v < 1.0 ) {
      throw new UnsupportedOperationException( "Cannot compute probabilities for " + numElements + " elements." )
    }

    // BloomSpecification.maxK min v.intValue
    v.intValue
  }


  case class BloomSpecification( numHashes: Int, bucketsPerElement: Double )
  object BloomSpecification {
    def apply( bucketsPerElement: Double ): BloomSpecification = BloomSpecification(
      calculateOptimalK( bucketsPerElement ),
      bucketsPerElement
    )

    def apply( maxBucketsPerElement: Double, maxFalsePositiveProbability: Double ): BloomSpecification = {
      require( maxBucketsPerElement >= 1.0 )
      val acceptableAccuracy = 1d - maxFalsePositiveProbability
      if ( acceptableAccuracy <= minKBucketAccuracy ) {
        BloomSpecification( calculateOptimalK(minBuckets), minBuckets )
      }
      else {
        val buckets: Option[Int] = ( minBuckets to maxBucketsPerElement.intValue ).find { curB =>
          val curK = calculateOptimalK( curB )
          calculateAccuracy( curK, curB ) >= acceptableAccuracy
        }

        var k: Option[Int] = None
        for ( b <- buckets ) {
          // Now that the number of buckets is sufficient, see if we can relax K
          // without losing too much precision.
          k = ( minK to calculateOptimalK(b) ).find { curK =>
            calculateAccuracy( curK, b ) >= acceptableAccuracy 
          }
        }

        ( k, buckets ) match {
          case ( Some(k), Some(buckets) ) => BloomSpecification( k, buckets )
          case ( _, _ ) => {
            throw new UnsupportedOperationException( 
              "Unable to satisfy %s with %s buckets per element".format( acceptableAccuracy, maxBucketsPerElement )
            )
          }
        }
      }
    }
    
    private[BloomFilter] val minBuckets = 2
    private[BloomFilter] val minK = 1
    private[BloomFilter] val maxK = 20
    lazy private[BloomFilter] val minKBucketAccuracy = calculateAccuracy( minK, minBuckets )
    private[BloomFilter] val EXCESS = 10
  }
  

//dmr: deserialize using kyro? protobuf? options?
  // def load( is: InputStream ) = {
  //   val buf = new Array[Byte]( 4 )
  //   
  //   is.read( buf )
  //   val capacity = convertToInt( buf )
  //   
  //   is.read( buf )
  //   val k = convertToInt( buf )
  //   
  //   is.read( buf )
  //   val width = convertToInt( buf )
  //   
  //   var buckets = BitSet()
  //   for ( _ <- 0 until (width / 8) ) {
  //     var num = is.read()
  //     var buf: List[Boolean] = Nil
  //     
  //     for ( _ <- 0 until 8 ) {
  //       buf = ( (num & 1) == 1 ) :: buf
  //       num >>= 1
  //     }
  //     
  //     buckets = buckets ++ buf
  //   }
  //   
  //   if ( width % 8 != 0 ) {
  //     var buf: List[Int] = Nil
  //     var num = is.read()
  //     
  //     for ( _ <- 0 until (width % 8) ) {
  //       buf = ( (num & 1) == 1 ) :: buf
  //       num >>= 1
  //     }
  //     
  //     buckets = buckets ++ buf
  //   }
  //   
  //   new BloomFilter( capacity, k, MurmurHashable, buckets )
  // }
  // 
  // 
  // private[collection] def convertToBytes( i: Int ) = {
  //   val buf = new Array[Byte]( 4 )
  // 
  //   buf(0) = ( (i & 0xff000000) >>> 24 ).byteValue
  //   buf(1) = ( (i & 0x00ff0000) >>> 16 ).byteValue
  //   buf(2) = ( (i & 0x0000ff00) >>> 8 ).byteValue
  //   buf(3) = ( i & 0x000000ff ).byteValue
  // 
  //   buf
  // }
  // 
  // private[collection] def convertToInt( buf: Array[Byte] ) = {
  //   ( (buf(0) & 0xFF) << 24 ) |
  //       ((buf(1) & 0xFF) << 16 ) |
  //       ( (buf(2) & 0xFF) << 8 ) |
  //       ( buf(3) & 0xFF )
  // }

}

trait Hashable extends Serializable {
  def hashes[T]( count: Int )( max: Int )( value: T ): Array[Int]
}


object MurmurHashable extends Hashable {
  import scala.util.hashing.{MurmurHash3 => MH}
  
  def hashes[T]( numHashes: Int )( max: Int )( value: T ): Array[Int] = {
    require( numHashes > 0 )
    require( max > 0 )

    val data = value.##
    val h1 = MH.mixLast( hash = ( numHashes ^ max ), data = data )
    val hash1 = MH.finalizeHash( hash = h1, length = 1 )
    val h2 = MH.mixLast( hash = hash1, data = data )
    val hash2 = MH.finalizeHash( hash = h2, length = 1 )

    ( for ( i <- 0 until numHashes ) yield math.abs( ( hash1 + ( i * hash2 ) ) % max ) ).toArray
  }
}
