import Dependencies._
import BuildSettings._

name := "omnibus"

version in ThisBuild := "0.70"

organization in ThisBuild := "com.github.dmrolfs"

// dependencies
lazy val root =
  ( project in file(".") )
  .enablePlugins( BuildInfoPlugin )
  .settings(
    buildInfoKeys := Seq[BuildInfoKey]( name, version, scalaVersion, sbtVersion ),
    buildInfoPackage := "omnibus"
  )
  .settings( publishArtifact := false )
  .aggregate( core, commons, identifier, archetype, akka )


lazy val core = ( project in file("./core") )
  .settings( defaultSettings )

lazy val identifier = ( project in file("./identifier") )
  .dependsOn( core )
  .settings( defaultSettings )

lazy val commons = ( project in file("./commons") )
  .dependsOn( core )
  .settings( defaultSettings )

lazy val archetype = ( project in file("./archetype") )
  .dependsOn( core, identifier, commons )
  .settings( defaultSettings )

lazy val akka = ( project in file("./akka") )
  .dependsOn( core, identifier, commons )
  .settings( defaultSettings )

scalafmtOnCompile in ThisBuild := true


//def isSnapshot( v: String ): Boolean = {
//  //  isSnapshot.value
//  v.endsWith( "-SNAPSHOT" )
//}

def doNotPublishSettings = Seq( publish := {} )

licenses := ("MIT", url("http://opensource.org/licenses/MIT")) :: Nil // this is required! otherwise Bintray will reject the code

credentials := List(Path.userHome / ".bintray" / ".jfrog-oss").filter(_.exists).map(Credentials(_))

resolvers += Resolver.url("omen bintray resolver", url("http://dl.bintray.com/omen/maven"))(Resolver.ivyStylePatterns)

publishMavenStyle := true

publishTo in ThisBuild := {
  if ( isSnapshot.value ) {
    Some("Artifactory Realm" at "http://oss.jfrog.org/artifactory/oss-snapshot-local")
  } else {
    Some("Bintray API Realm" at "http://api.bintray.com")
  }
}
//if ( isSnapshot( version.value ) ) {
//  publishTo in ThisBuild := {
//    Some("Artifactory Realm" at "http://oss.jfrog.org/artifactory/oss-snapshot-local")
//  }
//}


pomExtra := {
  <scm>
    <url>https://github.com</url>
    <connection>https://github.com/dmrolfs/omnibus.git</connection>
  </scm>
  <developers>
    <developer>
      <id>dmrolfs</id>
      <name>Damon Rolfs</name>
      <url>http://dmrolfs.github.io/</url>
    </developer>
  </developers>
}

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
