import sbt._
import Keys._


object Build extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val root = Project( "root", file( "." ) )
    .aggregate( commons, akka, archetype )
    .settings( basicSettings ++ doNotPublishSettings: _* )

  lazy val commons = Project( "peds-commons", file( "commons" ) )
    .settings( moduleSettings ++ publishSettings: _* )
    .settings( libraryDependencies ++=
      compile( "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4" ) ++
      compile( config ) ++
      compile( eaio ) ++
      compile( math3 ) ++
      compile( codec ) ++
      compile( logbackclassic ) ++
      compile( persistLogging ) ++
      compile( json4sJackson ) ++
      compile( scalalogging ) ++
      compile( joda ) ++
      compile( jodaConvert ) ++
      compile( scalazCore ) ++
      compile( scalazConcurrent ) ++
      compile( shapeless ) ++
      compile( shapelessBuilder ) ++
      compile( akkaActor ) ++
      test( scalatest ) ++
      test( scalacheck )
    )

  lazy val akka = Project( "peds-akka", file( "akka" ) )
    .dependsOn( commons )
    .settings( moduleSettings ++ publishSettings: _* )
    .settings( libraryDependencies ++=
      compile( config ) ++
      compile( akkaActor ) ++
      compile( akkaContrib ) ++
      compile( akkaPersistence ) ++
      compile( akkaSlf4j ) ++
      compile( akkaStreams ) ++
      compile( akkaAgent ) ++
      compile( persistLogging ) ++
      compile( fastutil ) ++
      compile( betterFiles ) ++
      compile( metricsCore ) ++
      compile( metricsGraphite ) ++
      compile( metricsScala ) ++
      test( akkaTestKit ) ++
      test( scalatest ) ++
      test( scalactic )
    )

  lazy val archetype = Project( "peds-archetype", file( "archetype" ) )
    .dependsOn( commons )
    .settings( moduleSettings ++ publishSettings: _* )
    .settings( libraryDependencies ++=
      compile( config ) ++
      compile( eaio ) ++
      compile( logbackclassic ) ++
      compile( json4sJackson ) ++
      compile( persistLogging ) ++
      compile( scalalogging ) ++
      compile( joda ) ++
      compile( jodaConvert ) ++
      compile( scalaTime ) ++
      compile( shapeless ) ++
      test( scalatest )
    )

  // lazy val slick = Project( "peds-slick", file( "slick" ) )
  //   .dependsOn( commons )
  //   .settings( moduleSettings: _* )
  //   .settings( libraryDependencies ++=
  //     compile( config ) ++
  //     compile( slickPersistence ) ++
  //     compile( mysqlConnector ) ++
  //     test( specs2 ) 
  //   )

  // lazy val spray = Project( "peds-spray", file( "spray" ) )
  //   .dependsOn( commons )
  //   .settings( moduleSettings: _* )
  //   .settings( libraryDependencies ++=
  //     compile( config ) ++
  //     compile( akkaActor ) ++
  //     compile ( akkaSlf4j ) ++
  //     compile( sprayCan ) ++
  //     compile( sprayRouting ) ++
  //     compile( json4sJackson ) ++
  //     test( specs2 ) 
  //   )

}
