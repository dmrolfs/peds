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
  .aggregate( core, commons, identifier, archetype, akka )


lazy val core = ( project in file("./core") )
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


//credentials in ThisBuild := {
//  if ( isSnapshot.value ) {
//    List(Path.userHome / ".bintray" / ".jfrog-oss").filter(_.exists).map(Credentials(_))
//  } else {
//    List(Path.userHome / ".bintray" / ".credentials").filter(_.exists).map(Credentials(_))
//  }
//}

publishMavenStyle in ThisBuild := true

//publishTo in ThisBuild := {
//  if ( isSnapshot.value ) {
//    println( "SETTING PUBLISH TO SNAPSHOT" )
//    Some("Artifactory Realm" at "http://oss.jfrog.org/artifactory/oss-snapshot-local")
//  } else {
//    println( "SETTING PUBLISH TO SNAPSHOT" )
//    Some("Bintray API Realm" at "http://api.bintray.com")
//  }
//}

//licenses in ThisBuild := ("MIT", url("http://opensource.org/licenses/MIT")) :: Nil // this is required! otherwise Bintray will reject the code

resolvers += Resolver.url("omen bintray resolver", url("http://dl.bintray.com/omen/maven"))(Resolver.ivyStylePatterns)

//pomExtra in ThisBuild := {
//  <scm>
//    <url>https://github.com</url>
//    <connection>https://github.com/dmrolfs/omnibus.git</connection>
//  </scm>
//  <developers>
//    <developer>
//      <id>dmrolfs</id>
//      <name>Damon Rolfs</name>
//      <url>http://dmrolfs.github.io/</url>
//    </developer>
//  </developers>
//}






//def publishSettings = {
//  if ( isSnapshot( version.value ) ) {
//    println( "PUBLISH_MODULE -- SNAPSHOT: " + version.value + " :: " + Credentials(Path.userHome / ".bintray" / ".jfrog-oss").toString  )
//    //    if ( VERSION.toString.endsWith("-SNAPSHOT") ) {
//    Seq(
//      publishTo := Some("Artifactory Realm" at "http://oss.jfrog.org/artifactory/oss-snapshot-local"),
//      publishMavenStyle := true,
//      // Only setting the credentials file if it exists (#52)
//      credentials := List(Path.userHome / ".bintray" / ".jfrog-oss").filter(_.exists).map(Credentials(_))
//    )
//  } else {
//    println( "PUBLISH_MODULE: " + version.value )
//    Seq(
//      },
//      publishMavenStyle := true,
//      resolvers += Resolver.url("omen bintray resolver", url("http://dl.bintray.com/omen/maven"))(Resolver.ivyStylePatterns),
//      licenses := ("MIT", url("http://opensource.org/licenses/MIT")) :: Nil // this is required! otherwise Bintray will reject the code
//    )
//  }
//}
//
