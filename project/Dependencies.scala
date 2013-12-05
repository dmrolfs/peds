import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "eaio.com" at "http://eaio.com/maven2",
    "dmrolfs" at "http://dmrolfs.github.com/snapshots",
    // "Mark Schaake" at "http://markschaake.github.com/snapshots",
    "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases"
  )

  def compile( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "compile" )
  def provided( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "provided" )
  def test( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "test" )
  def runtime( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "runtime" )
  def container( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "container" )

  val sprayVersion = "1.1-M7"
  def sprayModule( id: String ) = "io.spray" % id % sprayVersion

  val akkaVersion = "2.2.3"
  def akkaModule( id: String ) = "com.typesafe.akka" %% id % akkaVersion

  val akkaActor = akkaModule( "akka-actor" )
  val akkaSlf4j = akkaModule( "akka-slf4j" )
  val akkaTestKit = akkaModule( "akka-testkit" )

  val config = "com.typesafe" % "config" % "1.0.0"
  val eeioUUID = "com.eaio.uuid" % "uuid" % "3.4"
  val grizzledSlf4j = "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1"
  val joda = "joda-time" % "joda-time" % "2.2"
  val jodaConvert = "org.joda" % "joda-convert" % "1.3.1"
  val jscience = "org.jscience" % "jscience" % "4.3.1"
  val logbackclassic = "ch.qos.logback" % "logback-classic" % "1.0.13"
  val mysqlConnector = "mysql" % "mysql-connector-java" % "5.1.25"
  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % "0.8"
  val scalaTime = "scala-time" %% "scala-time" % "0.3.2"
  val shapeless = "com.chuusai" %% "shapeless" % "1.2.4"
  val slickPersistence = "com.typesafe.slick" %% "slick" % "1.0.0"
  val specs2 = "org.specs2" %% "specs2" % "2.1.1"
  val sprayHttp = sprayModule( "spray-http" )
  val sprayCan = sprayModule( "spray-can" )
  val sprayRouting = sprayModule( "spray-routing" )
  val sprayTestKit = sprayModule( "spray-testkit" )
  val sprayJson = "io.spray" %% "spray-json" % "1.2.5"
  val twirlApi = "io.spray" %% "twirl-api" % "0.6.1"
}