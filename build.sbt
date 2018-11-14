import Dependencies._
import BuildSettings._

name := "omnibus"

//version in ThisBuild := "0.72.1-SNAPSHOT"

organization in ThisBuild := "com.github.dmrolfs"

// dependencies
lazy val root =
  ( project in file(".") )
  .enablePlugins( BuildInfoPlugin )
  .settings(
    buildInfoKeys := Seq[BuildInfoKey]( name, version, scalaVersion, sbtVersion ),
    buildInfoPackage := "omnibus"
  )
  .settings( publish := {} )
  .aggregate( core, commons, lagom, identifier, archetype, akka )


lazy val core = ( project in file("./core") )
  .settings( defaultSettings ++ publishSettings )

lazy val lagom = ( project in file("./lagom" ) )
  .dependsOn( core )
  .settings( defaultSettings ++ publishSettings )

lazy val identifier = ( project in file("./identifier") )
  .dependsOn( core )
  .settings( defaultSettings ++ publishSettings )

lazy val commons = ( project in file("./commons") )
  .dependsOn( core )
  .settings( defaultSettings ++ publishSettings )

lazy val archetype = ( project in file("./archetype") )
  .dependsOn( core, identifier, commons )
  .settings( defaultSettings ++ publishSettings )

lazy val akka = ( project in file("./akka") )
  .dependsOn( core, identifier, commons )
  .settings( defaultSettings ++ publishSettings )

scalafmtOnCompile in ThisBuild := true


publishMavenStyle in ThisBuild := true


resolvers += Resolver.url("omen bintray resolver", url("http://dl.bintray.com/omen/maven"))(Resolver.ivyStylePatterns)
