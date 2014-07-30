package peds.commons.collection

import com.eaio.uuid.UUID
import util.Random
import org.specs2._
import com.typesafe.scalalogging.LazyLogging


object CommonStream {
  def ofInts( start: Int ): Stream[Int] = start #:: ofInts( start + 1 )
}


class BloomFilterUnitSpec extends mutable.Specification with LazyLogging {

  "A BloomFilter" should {
    "maintain size" in {
      var bf: BloomFilter[Int] = BloomFilter()
      bf.size === 0
      bf = bf + 1
      bf.size === 1
      bf = bf + 2
      bf.size === 2
      bf = bf + 2
      bf.size === 3
    }

    "find basic values" in {
      var bf = BloomFilter[String]()
      bf = bf + "Damon Rolfs"
      bf = bf + "Robin Moore"
      bf.has_?( "Damon Rolfs" ) must beTrue
      bf.has_?( "Robin Moore" ) must beTrue
    }
    
    "create bloom filter" in {
      val w = 70
      val k = 3
      val bf = BloomFilter[String]( 0.05, 10 )
      bf.k === k
      bf.width === w
    }
    
    "not find unset values" in {
      var bf = BloomFilter[String]()
      bf = bf + "Damon Rolfs"
      bf = bf + "Robin Moore"
      bf.has_?( "Shouldn't find" ) must beFalse
      bf.has_?( "Damon Rolf" ) must beFalse
    }
    
    "bloomfilters are deterministic" in {
      val v: Double = 3.1415926535897932384264
      val bf1 = BloomFilter[Double]( 0.01, 10 ) + v
      val bf2 = BloomFilter[Double]( 0.01, 10 ) + v
      bf1.has_?( v ) must beTrue
      bf2.has_?( v ) must beTrue
      bf1 === bf2
      
      val str = "Elena Rolfs Moore"
      val b = str.getBytes( "UTF-16" )
      val bfs = BloomFilter[String]( 0.05, 5 ) + str
      val bfb = BloomFilter[Array[Byte]]( 0.05, 5 ) + b
      bfs.width === 35
      bfs.width === bfb.width
      bfs.size === 1
      bfs.size === bfb.size
      bfs.has_?( str ) must beTrue
      bfb.has_?( b ) must beTrue
      bfb.has_?( str.getBytes ) must beFalse
      
      bfs.has_?( new String(b, "UTF-16") ) must beTrue
      bfb.has_?( str.getBytes("UTF-16") ) must beFalse
    }
    
    "mass set should all be recognized" in {
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
      hit_ratio must be_<=( 1d )
    }
    
    "support add all" in {
      import KeyGenerator._
      val sampleSize = 10000
      val ukeys: List[String] = (randomUUIDs take sampleSize).toList
      var bf = BloomFilter[String]( .05, sampleSize ) ++ ukeys
      ukeys.size === sampleSize
      bf.size === sampleSize
      
      val count = ukeys count { bf has_? _ }
      count === sampleSize
    }
    
    "knows size" in {
      import KeyGenerator._
      val sampleSize = 10000
      val randomKeys: List[String] = ( randomStrings( new Random(314159) ) take sampleSize ).toList
      var bf: BloomFilter[String] = BloomFilter( 0.05, sampleSize )
      bf.size === 0
      randomKeys.size === sampleSize
      bf = bf ++ randomKeys
      bf.size === sampleSize
    }
    
    "satisfy false positive random" in {
      import KeyGenerator._
      val sampleSize = 10000
      val acceptableFailureRate = 0.1
      val randomKeys: Set[String] = ( randomStrings( new Random(314159) ) take sampleSize ).toSet
      var bf: BloomFilter[String] = BloomFilter( acceptableFailureRate, sampleSize )
      bf.size === 0
      randomKeys.size === sampleSize
      bf = bf ++ randomKeys
      bf.size === sampleSize
      val maxFailureRate: Double = 1d - bf.accuracy
      // trace( "maxFailureRate="+maxFailureRate )
      val randomInquiries = randomStrings( new Random(271828) ) take sampleSize
      val copies = randomInquiries count { randomKeys( _ ) }
      val fp = ( randomInquiries.toList count { bf( _ ) } ) - copies
      val fp_ratio: Double = fp.doubleValue / randomInquiries.size
      // trace( "fp="+fp )
      // trace( "fp_ratio="+fp_ratio )
      fp_ratio must be_<=( acceptableFailureRate )
    }
    
    "satisfy false positive int" in {
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
       fp_ratio must be_<=( acceptableFailureRate )
     }
     
     "satisfy word false positives" in {
       import KeyGenerator._
       WordGenerator.size must be_>( 0 )
     
       val sampleSize = 10000
       val acceptableFailureRate = 0.1
       val keys1: Set[String] = (randomWords take sampleSize).toSet
       // trace( "keys1.size="+keys1.size )

       var bf = BloomFilter[String]( acceptableFailureRate, keys1.size )
       bf = bf ++ keys1
       bf.size === keys1.size
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
       fp_ratio must be_<=( acceptableFailureRate )
     }
     
     "support equality" in {
       import KeyGenerator._
       val sampleSize = 10000
       val keys = ( randomInts( sampleSize * 100 ) take sampleSize ).toList
       keys.size === sampleSize
       val bf1 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
       val bf2 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
       bf1.size === keys.size
       bf2.size === keys.size
       bf1 === bf2
       
       val bf3 = BloomFilter[Int]( 0.1, keys.size ) ++ keys
       bf3.size === keys.size
       bf1 !=== bf3
       bf3 !=== bf2
       
       val pi = 3.1415926535897932384264
       val bfp1 = BloomFilter[Double]() + pi
       val bfp2 = BloomFilter[Double]() + 3.1415926535897932384264
       logger.info( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" )
       bfp1 === bfp2
     }
     
     "has a deterministic hashcode" in {
       import KeyGenerator._
       val keys = ( randomInts( 1000000 ) take 10000 ).toList
       val bf1 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
       val bf2 = BloomFilter[Int]( 0.05, keys.size ) ++ keys
       bf1.hashCode === bf2.hashCode
       
       val bf3 = BloomFilter[Int]( 0.1, keys.size ) ++ keys
       bf1.hashCode !=== bf3.hashCode
       bf3.hashCode !=== bf2.hashCode
     }
     
     "be seriazable" in {
       todo
     }
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