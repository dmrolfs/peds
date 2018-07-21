import Dependencies._
import BuildSettings._

name := "omnibus"
organization in ThisBuild := "com.github.dmrolfs"
scalaVersion in ThisBuild := "2.12.4"

crossScalaVersions in ThisBuild := Seq("2.11.12")

// ivyScala := ivyScala.value map {
//   _.copy(overrideScalaVersion = true)
// }

// replaces dynver + by -
version in ThisBuild ~= (_.replace('+', '-'))

// dependencies
lazy val root =
  ( project in file(".") )
  .enablePlugins( BuildInfoPlugin )
  .settings(
    buildInfoKeys := Seq[BuildInfoKey]( name, version, scalaVersion, sbtVersion ),
    buildInfoPackage := "omnibus"
  )
  .settings( publishArtifact := false )
  .aggregate( commons, archetype )


lazy val commons = ( project in file("./commons") )
  .settings( defaultSettings ++ publishSettings:_* )

lazy val archetype = ( project in file("./archetype") )
  .dependsOn( commons )
  .settings( defaultSettings ++ publishSettings:_* )

lazy val akka = ( project in file("./akka") )
  .dependsOn( commons )
  .settings( defaultSettings ++ publishSettings:_* )

scalafmtOnCompile in ThisBuild := true
