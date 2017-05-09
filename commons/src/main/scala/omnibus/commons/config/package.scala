package omnibus.commons

import java.net.InetAddress
import scala.concurrent.duration.{ Duration, FiniteDuration }
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import squants.information.{ Bytes, Information }


package object config {
  implicit val inetAddressReader = new ValueReader[InetAddress] {
    override def read( config: Config, path: String ): InetAddress = InetAddress.getByName( config.as[String]( path ) )
  }

  implicit val durationReader = new ValueReader[Duration] {
    override def read( config: Config, path: String ): Duration = {
      if ( config.as[String]( path ).trim.compareToIgnoreCase( "Inf" ) == 0 ) Duration.Inf
      else config.as[FiniteDuration]( path ).toCoarsest
    }
  }

  implicit val informationReader = new ValueReader[Information] {
    override def read( config: Config, path: String ): Information = Bytes( config.getMemorySize( path ).toBytes )
  }
}
