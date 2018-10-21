import BuildSettings._
import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.MergeStrategy

name := "omnibus-lagom"

description := "lorem ipsum."

scalacOptions := scalacBuildOptions

resolvers += "omen-bintray" at "http://dl.bintray.com/omen/maven"

libraryDependencies ++=
  circe.all ++
  lagom.all ++
  Seq(
    akka.actor
  )

testOptions in Test += Tests.Argument( "-oDF" )

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = false)

assemblyJarName in assembly := s"${organizationName.value}-${name.value}-${version.value}.jar"
