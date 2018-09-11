import BuildSettings._
import Dependencies._

name := "omnibus-identifier"

description := "lorem ipsum."

scalacOptions := scalacBuildOptions

resolvers += "omen-bintray" at "http://dl.bintray.com/omen/maven"

libraryDependencies ++=
  logging.all ++
  Seq(
    facility.scalaUuid,
    cats.core,
    facility.codec,
    facility.scalaUuid,
    facility.newtype
  ) ++
  Dependencies.commonTestDependencies

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

testOptions in Test += Tests.Argument( "-oDF" )

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = false)

assemblyJarName in assembly := s"${organizationName.value}-${name.value}-${version.value}.jar"