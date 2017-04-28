import sbt._
import Keys._
import org.scoverage.coveralls.Imports.CoverallsKeys._


object BuildSettings {

  val VERSION = "0.5.3-SNAPSHOT"

  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := ()
  )

  lazy val basicSettings = Seq(
    version := VERSION,
    organization := "com.github.dmrolfs",
    description := "A Scala library providing common and generally applicable support for system development, including utilities, data structures, algorithms and archetypes.",
    startYear := Some(2013),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    scalaVersion := "2.12.2",
    resolvers ++= Dependencies.resolutionRepos,
    resolvers += Resolver.jcenterRepo,
    coverallsTokenFile := Some("~/.sbt/omnibus-coveralls-token.txt"),
    scalacOptions := Seq(
      "-encoding",
      "utf8",
      // "-Xlog-implicits",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args"
    ),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

  lazy val moduleSettings = basicSettings ++ Seq(
    version := VERSION,
    isSnapshot := true
//    publishTo := Some( Resolver.file("file", new File( Path.userHome.absolutePath + "/jd/dev/dmrolfs.github.com/snapshots" ) ) )
  )

  def doNotPublishSettings = Seq(publish := {})

  def publishSettings = {
    // if ( (version in ThisBuild).toString.endsWith("-SNAPSHOT") ) {
    if ( (VERSION).toString.endsWith("-SNAPSHOT") ) {
      Seq(
        publishTo := Some("Artifactory Realm" at "http://oss.jfrog.org/artifactory/oss-snapshot-local"),
        publishMavenStyle := true,
        // Only setting the credentials file if it exists (#52)
        credentials := List(Path.userHome / ".bintray" / ".artifactory").filter(_.exists).map(Credentials(_))
      )
    } else {
      Seq(
        pomExtra := <scm>
          <url>https://github.com</url>
          <connection>https://github.com/dmrolfs/omnibus.git</connection>
        </scm>
        <developers>
          <developer>
            <id>dmrolfs</id>
            <name>Damon Rolfs</name>
            <url>http://dmrolfs.github.io/</url>
          </developer>
        </developers>,
        publishMavenStyle := true,
        resolvers += Resolver.url("omen bintray resolver", url("http://dl.bintray.com/omen/maven"))(Resolver.ivyStylePatterns),
        licenses := ("MIT", url("http://opensource.org/licenses/MIT")) :: Nil // this is required! otherwise Bintray will reject the code
      )
    }
  }
}
