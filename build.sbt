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

// // Akka integration ==============================
// lazy val funCqrsAkka = Project(
//   id       = "fun-cqrs-akka",
//   base     = file("modules/akka"),
//   settings = defaultSettings
// ).settings(libraryDependencies ++= mainDeps ++ akkaDeps)
//   .dependsOn(funCqrs % "compile->compile;test->test")
// //================================================

// //Test kit =======================================
// lazy val funCqrsTestKit = Project(
//   id       = "fun-cqrs-test-kit",
//   base     = file("modules/tests"),
//   settings = defaultSettings
// ).settings(libraryDependencies ++= mainDeps ++ Seq(rxScala, reactiveStreamAdapter))
//   .dependsOn(funCqrs % "compile->compile;test->test")
// //================================================

// // #####################################################
// // #                     SAMPLES                      #
// // #####################################################
// lazy val raffleApp = Project(
//   id       = "sample-raffle",
//   base     = file("samples/raffle"),
//   settings = defaultSettings
// ).settings(libraryDependencies ++= sampleDeps)
//   .settings(publishArtifact := false)
//   .dependsOn(funCqrs)
//   .dependsOn(funCqrsTestKit)
//   .dependsOn(funCqrsAkka)

// addCommandAlias("runRaffleAkka", "sample-raffle/runMain raffle.app.MainAkka")
// addCommandAlias("runRaffleInMemory", "sample-raffle/runMain raffle.app.MainInMemory")
