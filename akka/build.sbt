import BuildSettings._
import Dependencies._
import sbtassembly.AssemblyPlugin.autoImport.MergeStrategy

name := "omnibus-akka"

description := "lorem ipsum."

scalacOptions := scalacBuildOptions

resolvers += "omen-bintray" at "http://dl.bintray.com/omen/maven"

libraryDependencies ++=
  commonDependencies ++
  akka.all ++
  betterFiles.all ++
  metrics.all ++
//  monix.all ++
//  time.all ++
  Seq(
    akka.persistenceQuery,
    persistence.cassandra,
    facility.fastutil
  ) ++
  commonTestDependencies

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

testOptions in Test += Tests.Argument( "-oDF" )

//import Tests._
//
//{
//  def groupByFirst(tests: Seq[TestDefinition]) =
//    tests groupBy (_.name(0)) map {
//      case (letter, tests) =>
//        val options = ForkOptions().withRunJVMOptions(Vector("-Dfirst.letter"+letter))
//        new Group(letter.toString, tests, SubProcess(options))
//    } toSeq
//
//  testGrouping in Test := groupByFirst( (definedTests in Test).value )
//}
