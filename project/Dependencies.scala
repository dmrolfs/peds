import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "eaio.com" at "http://repo.eaio.com/maven2",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "omen-bintray" at "http://dl.bintray.com/omen/maven",
    "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases",
    "dl-john-ky" at "http://dl.john-ky.io/maven/releases",
    "OSS JFrog Artifactory" at "http://oss.jfrog.org/artifactory/oss-snapshot-local"
  )

  def compile( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "compile" )
  def provided( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "provided" )
  def test( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "test" )
  def runtime( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "runtime" )
  def container( deps: ModuleID* ): Seq[ModuleID] = deps map ( _ % "container" )

  val akkaVersion = "2.4.17"
  def akkaModule( id: String ) = "com.typesafe.akka" %% id % akkaVersion

  val akkaActor = akkaModule( "akka-actor" )
  val akkaContrib = akkaModule( "akka-contrib" )
  val akkaPersistence = akkaModule( "akka-persistence" )
  val akkaSlf4j = akkaModule( "akka-slf4j" )
  val akkaTestKit = akkaModule( "akka-testkit" )
  val akkaStreams = akkaModule( "akka-stream" )
  val akkaAgent = akkaModule( "akka-agent" )
  val akkaQuery = akkaModule( "akka-persistence-query-experimental" )

  val config = "com.typesafe" % "config" % "1.3.1"
  val ficus = "com.iheart" %% "ficus" % "1.4.0"
  val eaio = "com.github.stephenc.eaio-uuid" % "uuid" % "3.4.0"
  val math3 = "org.apache.commons" % "commons-math3" % "3.6.1"
  val codec = "commons-codec" % "commons-codec" % "1.10"
  val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.5.0"
  val scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  val logbackclassic = "ch.qos.logback" % "logback-classic" % "1.2.1"
  val shapelessBuilder = "com.github.dmrolfs" %% "shapeless-builder" % "1.0.1"
  val fastutil = "it.unimi.dsi" % "fastutil" % "7.0.13" withSources() withJavadoc()
  val persistLogging = "com.persist" %% "persist-logging" % "1.3.1"
  // val hashids = "com.github.dmrolfs" %% "hashids-scala" % "1.1.2-9ff5999"
  val squants = "org.typelevel"  %% "squants"  % "1.1.0"

  val joda = "joda-time" % "joda-time" % "2.9.7"
  val jodaConvert = "org.joda" % "joda-convert" % "1.8.1"

  val betterFiles = "com.github.pathikrit" %% "better-files" % "2.17.1"
  val metricsCore = "io.dropwizard.metrics" % "metrics-core" % "3.1.2"
  val metricsGraphite = "io.dropwizard.metrics" % "metrics-graphite" % "3.1.2"
  val metricsScala = "nl.grons" %% "metrics-scala" % "3.5.5_a2.4"

  val cassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.22"
  val leveldb = "org.iq80.leveldb" % "leveldb" % "0.7" // "org.iq80.leveldb" % "leveldb" % "0.9"
  val leveldbjni = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" // "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

  val scalaTime = "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"
  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.2.8"
  val scalazConcurrent = "org.scalaz" %% "scalaz-concurrent" % "7.2.8"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"
  val scalactic = "org.scalactic" %% "scalactic" % "3.0.1"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4"
}
