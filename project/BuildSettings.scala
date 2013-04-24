import sbt._
import Keys._

object BuildSettings {

  val VERSION = "0.1.3"

  lazy val noPublishing = seq(
    publish := (),
    publishLocal := ()
  )

  lazy val basicSettings = seq(
    version := VERSION,
    organization := "Damon Rolfs",
    description := "A Scala library providing common and generally applicable support for system development, including utilities, data structures, algorithms and archetypes.",
    startYear := Some(2013),
    licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion := "2.10.1",
    resolvers ++= Dependencies.resolutionRepos,
    scalacOptions := Seq(
      "-encoding", 
      "utf8",
      // "-Xlog-implicits",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args"
    )
  )

  lazy val moduleSettings = basicSettings ++ seq(
    version := VERSION,
    isSnapshot := true,
    publishTo := Some( Resolver.file("file", new File(Path.userHome.absolutePath + "/projects/dmrolfs.github.com/snapshots")) )
  )
}
