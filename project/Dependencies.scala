import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "eaio.com" at "http://repo.eaio.com/maven2",
//    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
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

  val akkaVersion = "2.5.1"
  def akkaModule( id: String ) = "com.typesafe.akka" %% id % akkaVersion

  val akkaActor = akkaModule( "akka-actor" )
  val akkaContrib = akkaModule( "akka-contrib" )
  val akkaPersistence = akkaModule( "akka-persistence" )
  val akkaSlf4j = akkaModule( "akka-slf4j" )
  val akkaTestKit = akkaModule( "akka-testkit" )
  val akkaStreams = akkaModule( "akka-stream" )
  val akkaAgent = akkaModule( "akka-agent" )
  val akkaQuery = akkaModule( "akka-persistence-query" )

  val config = "com.typesafe" % "config" % "1.3.1"
  val ficus = "com.iheart" %% "ficus" % "1.4.0"
  val eaio = "com.github.stephenc.eaio-uuid" % "uuid" % "3.4.0"
  val math3 = "org.apache.commons" % "commons-math3" % "3.6.1"
  val codec = "commons-codec" % "commons-codec" % "1.10"
  val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.5.2"
  val scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  val logbackclassic = "ch.qos.logback" % "logback-classic" % "1.2.2"
  val shapelessBuilder = "com.github.dmrolfs" %% "shapeless-builder" % "1.0.1"
  val fastutil = "it.unimi.dsi" % "fastutil" % "7.2.0" withSources() withJavadoc()
  val persistLogging = "com.persist" %% "persist-logging" % "1.3.2"
  // val hashids = "com.github.dmrolfs" %% "hashids-scala" % "1.1.2-9ff5999"
  val squants = "org.typelevel"  %% "squants"  % "1.2.0"

  val joda = "joda-time" % "joda-time" % "2.9.9"
  val jodaConvert = "org.joda" % "joda-convert" % "1.8.1"

  val betterFiles = "com.github.pathikrit" %% "better-files" % "3.0.0"
  val metricsCore = "io.dropwizard.metrics" % "metrics-core" % "3.2.2"
  val metricsGraphite = "io.dropwizard.metrics" % "metrics-graphite" % "3.2.2"
  val metricsScala = "nl.grons" %% "metrics-scala" % "3.5.6_a2.4"

  val cassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.52"
  val leveldb = "org.iq80.leveldb" % "leveldb" % "0.9" // "org.iq80.leveldb" % "leveldb" % "0.9"
  val leveldbjni = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" // "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

  val scalaTime = "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"

  object Cats {
    val version = "0.9.0"
    def module( id: String ) = "org.typelevel" %% s"cats-${id}" % version

    val core = module( "core" )
    val kernel = module( "kernel" )
    val macros = module( "macros" )

    val all = Seq( core, kernel, macros )
  }

  object Monix {
    val version = "2.3.0"
    def module( id: String ) = "io.monix" %% s"""monix${if (id.nonEmpty) '-'+id else "" }""" % version

    val core = module( "" )
    val cats = module( "cats" )

    val all = Seq( core, cats )
  }
//  val scalazCore = "org.scalaz" %% "scalaz-core" % "7.2.8"
//  val scalazConcurrent = "org.scalaz" %% "scalaz-concurrent" % "7.2.8"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1"
  val scalactic = "org.scalactic" %% "scalactic" % "3.0.1"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4"
}
