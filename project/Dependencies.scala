import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "eaio.com" at "http://eaio.com/maven2",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "omen-bintray" at "http://dl.bintray.com/omen/maven",
    "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases"
  )

  def compile( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "compile" )
  def provided( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "provided" )
  def test( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "test" )
  def runtime( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "runtime" )
  def container( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "container" )

  val akkaVersion = "2.4.5"
  def akkaModule( id: String ) = "com.typesafe.akka" %% id % akkaVersion

  val akkaActor = akkaModule( "akka-actor" )
  val akkaContrib = akkaModule( "akka-contrib" )
  val akkaPersistence = akkaModule( "akka-persistence" )
  val akkaSlf4j = akkaModule( "akka-slf4j" )
  val akkaTestKit = akkaModule( "akka-testkit" )
  val akkaStreams = akkaModule( "akka-stream" )

  val config = "com.typesafe" % "config" % "1.3.0"
  val eaio = "com.eaio.uuid" % "uuid" % "3.4"
  val math3 = "org.apache.commons" % "commons-math3" % "3.5"
  val codec = "commons-codec" % "commons-codec" % "1.10"
  val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.3.0"
  val scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
  val logbackclassic = "ch.qos.logback" % "logback-classic" % "1.1.3"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.4"
  val shapelessBuilder = "com.github.dmrolfs" %% "shapeless-builder" % "1.0.0"

  val joda = "joda-time" % "joda-time" % "2.9.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.8"

  val betterFiles = "com.github.pathikrit" %% "better-files" % "2.14.0"
  val metricsCore = "io.dropwizard.metrics" % "metrics-core" % "3.1.2"
  val metricsGraphite = "io.dropwizard.metrics" % "metrics-graphite" % "3.1.2"
  val metricsScala = "nl.grons" %% "metrics-scala" % "3.5.2_a2.3"

  val scalaTime = "com.github.nscala-time" %% "nscala-time" % "2.6.0"
  val shapeless = "com.chuusai" %% "shapeless" % "2.2.5"
  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.2.0"
}