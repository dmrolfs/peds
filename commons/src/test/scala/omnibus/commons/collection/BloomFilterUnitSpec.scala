package omnibus.commons.collection

import com.eaio.uuid.UUID
import util.Random
import org.scalatest._
import org.scalatest.Matchers
import com.typesafe.scalalogging.LazyLogging


object CommonStream {
  def ofInts( start: Int ): Stream[Int] = start #:: ofInts( start + 1 )
}


class BloomFilterUnitSpec extends FlatSpec with Matchers with LazyLogging {

  "A BloomFilter" should "maintain size" in {
    var bf: BloomFilter[Int] = BloomFilter()
    bf.size shouldBe 0
    bf = bf + 1
    bf.size shouldBe 1
    bf = bf + 2
    bf.size shouldBe 2
    bf = bf + 2
    bf.size shouldBe 3
  }

  it should "find basic values" in {
    var bf = BloomFilter[String]()
    bf = bf + "Damon Rolfs"
    bf = bf + "Robin Moore"
    bf.has_?( "Damon Rolfs" ) should be (true)
    bf.has_?( "Robin Moore" ) should be (true)
  }
  
  it should "create bloom filter" in {
    val w = 70
    val k = 3
    val bf = BloomFilter[String]( 0.05, 10 )
    bf.k shouldBe k
    bf.width shouldBe w
  }
  
  it should "not find unset values" in {
    var bf = BloomFilter[String]()
    bf = bf + "Damon Rolfs"
    bf = bf + "Robin Moore"
    bf.has_?( "Shouldn't find" ) should be (false)
    bf.has_?( "Damon Rolf" ) should be (false)
  }
  
  it should "bloomfilters are deterministic" in {
    val v: Double = 3.1415926535897932384264
    val bf1 = BloomFilter[Double]( 0.01, 10 ) + v
    val bf2 = BloomFilter[Double]( 0.01, 10 ) + v
    bf1.has_?( v ) should be (true)
    bf2.has_?( v ) should be (true)
    bf1 shouldBe bf2
    
    val str = "Elena Rolfs Moore"
    val b = str.getBytes( "UTF-16" )
    val bfs = BloomFilter[String]( 0.05, 5 ) + str
    val bfb = BloomFilter[Array[Byte]]( 0.05, 5 ) + b
    bfs.width shouldBe 35
    bfs.width shouldBe bfb.width
    bfs.size shouldBe 1
    bfs.size shouldBe bfb.size
    bfs.has_?( str ) should be (true)
    bfb.has_?( b ) should be (true)
    bfb.has_?( str.getBytes ) should be (false)
    
    bfs.has_?( new String(b, "UTF-16") ) should be (true)
    bfb.has_?( str.getBytes("UTF-16") ) should be (false)
  }
  
  it should "mass set should all be recognized" in {
    import KeyGenerator._
    val randomKeys: List[Int] = ( CommonStream.ofInts( 0 ) take 10000 ).toList
    var bf: BloomFilter[Int] = BloomFilter( 0.005, 10000 )
    val maxFailureRate: Double = 1d - bf.accuracy
    bf = bf ++ randomKeys
  
    val hits = randomKeys count { bf has_? _.hashCode }
    val hit_ratio: Double = hits.doubleValue / randomKeys.size
    // trace( "maxFailureRate="+maxFailureRate )
    // trace( "hits="+hits )
    // trace( "hit_ratio="+hit_ratio )
    hit_ratio should be <= 1d 
  }
  
  it should "support add all" in {
    import KeyGenerator._
    val sampleSize = 10000
    val ukeys: List[String] = (randomUUIDs take sampleSize).toList
    var bf = BloomFilter[String]( .05, sampleSize ) ++ ukeys
    ukeys.size shouldBe sampleSize
    bf.size shouldBe sampleSize
    
    val count = ukeys count { bf has_? _ }
    count shouldBe sampleSize
  }
  
  it should "knows size" in {
    import KeyGenerator._
    val sampleSize = 10000
    val randomKeys: List[String] = ( randomStrings( new Random(314159) ) take sampleSize ).toList
    var bf: BloomFilter[String] = BloomFilter( 0.05, sampleSize )
    bf.size shouldBe 0
    randomKeys.size shouldBe sampleSize
    bf = bf ++ randomKeys
    bf.size shouldBe sampleSize
  }
  
  it should "satisfy false positive random" in {
    import KeyGenerator._
    val sampleSize = 10000
    val acceptableFailureRate = 0.1
    val randomKeys: Set[String] = ( randomStrings( new Random(314159) ) take sampleSize ).toSet
    var bf: BloomFilter[String] = BloomFilter( acceptableFailureRate, sampleSize )
    bf.size shouldBe 0
    randomKeys.size shouldBe sampleSize
    bf = bf ++ randomKeys
    bf.size shouldBe sampleSize
    val maxFailureRate: Double = 1d - bf.accuracy
    // trace( "maxFailureRate="+maxFailureRate )
    val randomInquiries = randomStrings( new Random(271828) ) take sampleSize
    val copies = randomInquiries count { randomKeys( _ ) }
    val fp = ( randomInquiries.toList count { bf( _ ) } ) - copies
    val fp_ratio: Double = fp.doubleValue / randomInquiries.size
    // trace( "fp="+fp )
    // trace( "fp_ratio="+fp_ratio )
    fp_ratio should be <= acceptableFailureRate
  }
  
  it should "satisfy false positive int" in {
     import KeyGenerator._
     val sampleSize = 10000
     val acceptableFailureRate = 0.1
     val randomKeys: Set[Int] = ( CommonStream.ofInts( 0 ) take sampleSize ).toSet
     var bf: BloomFilter[Int] = BloomFilter( acceptableFailureRate, sampleSize )
     bf = bf ++ randomKeys
     val maxFailureRate: Double = 1d - bf.accuracy
     // trace( "maxFailureRate="+maxFailureRate )
     val randomInquiries = randomInts() take sampleSize
     val copies = randomInquiries count { randomKeys( _ ) }
     val fp = (randomInquiries count { bf has_? _.hashCode }) - copies
     val fp_ratio: Double = fp.doubleValue / randomInquiries.size
     // trace( "fp="+fp )
     // trace( "fp_ratio="+fp_ratio )
     fp_ratio should be <= acceptableFailureRate 
   }
   
   it should "satisfy word false positives" in {
     import KeyGenerator._
     WordGenerator.size should be > 0
   
     val sampleSize = 10000
     val acceptableFailureRate = 0.1
     val keys1: Set[String] = (randomWords take sampleSize).toSet
     // trace( "keys1.size="+keys1.size )

     var bf = BloomFilter[String]( acceptableFailureRate, keys1.size )
     bf = bf ++ keys1
     bf.size shouldBe keys1.size
     val maxFailureRate: Double = 1d - bf.accuracy
     // trace( "maxFailureRate="+maxFailureRate )
     
     val rkeys: List[String] = (randomWords take sampleSize).toList
     // trace( "rkeys.size="+rkeys.size )
     val copies = rkeys count { keys1( _ ) }
     // trace( "copies="+copies )
     val fp = ( rkeys count { bf has_? _ } ) - copies
     val fp_ratio: Double = fp.doubleValue / rkeys.size
     // trace( "fp="+fp )
     // trace( "fp_ratio="+fp_ratio )
     fp_ratio should be <= acceptableFailureRate
   }
   
   it should "support equality" in {
     import KeyGenerator._
     val sampleSize = 10000
     val keys = ( randomInts( sampleSize * 100 ) take sampleSize ).toList
     keys.size shouldBe sampleSize
     val bf1 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
     val bf2 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
     bf1.size shouldBe keys.size
     bf2.size shouldBe keys.size
     bf1 shouldBe bf2
     
     val bf3 = BloomFilter[Int]( 0.1, keys.size ) ++ keys
     bf3.size shouldBe keys.size
     bf1 should not be bf3
     bf3 should not be bf2
     
     val pi = 3.1415926535897932384264
     val bfp1 = BloomFilter[Double]() + pi
     val bfp2 = BloomFilter[Double]() + 3.1415926535897932384264
     logger.info( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" )
     bfp1 shouldBe bfp2
   }
   
   it should "has a deterministic hashcode" in {
     import KeyGenerator._
     val keys = ( randomInts( 1000000 ) take 10000 ).toList
     val bf1 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
     val bf2 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
     bf1.hashCode shouldBe bf2.hashCode
     
     val bf3 = BloomFilter[Int]( 0.1, keys.size ) ++ keys
     bf1.hashCode should not be bf3.hashCode
     bf3.hashCode should not be bf2.hashCode
   }
   
   it should "be seriazable" in {
     pending
   }
}

// public static Filter testSerialize(Filter f) throws IOException
// {
//     f.add(ByteBufferUtil.bytes("a"));
//     DataOutputBuffer out = new DataOutputBuffer();
//     FilterFactory.serialize(f, out, FilterFactory.Type.MURMUR3);
// 
//     ByteArrayInputStream in = new ByteArrayInputStream(out.getData(), 0, out.getLength());
//     Filter f2 = FilterFactory.deserialize(new DataInputStream(in), FilterFactory.Type.MURMUR3);
// 
//     assert f2.isPresent(ByteBufferUtil.bytes("a"));
//     assert !f2.isPresent(ByteBufferUtil.bytes("b"));
//     return f2;
// }
// 
// 
// @Test
// public void testSerialize() throws IOException
// {
//     BloomFilterTest.testSerialize(bf);
// }
// 
  // /**
  //  * Test of contains method, of class BloomFilter.
  //  * @throws Exception
  //  */
  // @Test
  // public void testContains() throws Exception {
  //     System.out.println("contains");
  //     BloomFilter instance = new BloomFilter(10000, 10);
  // 
  //     for (int i = 0; i < 10; i++) {
  //         instance.add(Integer.toBinaryString(i));
  //         assert(instance.contains(Integer.toBinaryString(i)));
  //     }
  // 
  //     assertFalse(instance.contains(UUID.randomUUID().toString()));
  // }
  // 
object KeyGenerator {
  private val r = new Random()
  
  def randomKey( r: Random ) : Array[Byte] = {
    val result = new Array[Byte]( 48 )
    r.nextBytes( result )
    result
  }

  def randomBytes( buffer: Array[Byte] ): Stream[Array[Byte]] = {
    Random.nextBytes( buffer )
    buffer #:: randomBytes( buffer )
  }
  
  def randomInts( bound: Int = (Int.MaxValue / 2) ): Stream[Int] = {
    ( Random.nextInt(bound * 2) - bound ) #:: randomInts( bound )
  }
  
  def randomStrings( r: Random ): Stream[String] = new String(randomKey(r), "UTF-16" ) #:: randomStrings( r )
  def randomWords: Stream[String] = WordGenerator.WORDS( Random.nextInt(WordGenerator.size) ) #:: randomWords
  def randomUUIDs: Stream[String] = (new UUID).toString #:: randomUUIDs
  
  object WordGenerator {
    import java.nio.charset._
    import java.nio.file._
    import scala.collection.JavaConversions._
    
    lazy val WORDS: Array[String] = {
      try {
        val words: Seq[String] = Files.readAllLines( 
          Paths.get("/", "usr", "share", "dict", "words"),
          Charset.forName( "UTF-8" )
        )
        words.toArray
      } catch {
        case _: Throwable => Array()
      }
    }
    
    lazy val size = WORDS.size
  }
}