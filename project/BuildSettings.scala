import sbt._
import Keys._
import CoverallsPlugin.CoverallsKeys._
import scoverage.ScoverageSbtPlugin.instrumentSettings
import org.scoverage.coveralls.CoverallsPlugin.coverallsSettings


object BuildSettings {

  val VERSION = "0.1.6"

  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := ()
  )

  lazy val basicSettings = Seq(
    version := VERSION,
    organization := "com.github.dmrolfs",
    description := "A Scala library providing common and generally applicable support for system development, including utilities, data structures, algorithms and archetypes.",
    startYear := Some(2013),
    licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion := "2.11.2",
    resolvers ++= Dependencies.resolutionRepos,
    coverallsTokenFile := "~/.sbt/peds-coveralls-token.txt",
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

  lazy val moduleSettings = basicSettings ++ instrumentSettings ++ coverallsSettings ++ Seq(
    version := VERSION,
    isSnapshot := true,
    publishTo := Some( Resolver.file("file", new File( Path.userHome.absolutePath + "/dev/dmrolfs.github.com/snapshots" ) ) )
  )
}
