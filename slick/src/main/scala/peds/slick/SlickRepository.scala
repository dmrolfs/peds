package dr.commons.slick

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.session.Database
// import scala.slick.session.Database.threadLocalSession
import com.typesafe.config.{Config, ConfigFactory}


trait SlickRepository {
  def database: Database = SlickRepository.db
}

object SlickRepository {
  lazy val config: Config = ConfigFactory.load

  val db = Database.forURL( url = config.getString( "peds.jdbc.url" ), driver = config.getString( "peds.jdbc.driver" ) )
}