import sbt._
import Keys._


object Build extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val root = Project( "root", file( "." ) )
    .aggregate( commons, akka, archetype )
    .settings( basicSettings: _* )
    .settings( noPublishing: _* )

  lazy val commons = Project( "peds-commons", file( "commons" ) )
    .settings( moduleSettings: _* )
    .settings( libraryDependencies ++=
      compile( config ) ++
      compile( eaio ) ++
      compile( codec ) ++
      compile( logbackclassic ) ++
      compile( json4sJackson ) ++
      compile( scalalogging ) ++
      compile( joda ) ++
      compile( jodaConvert ) ++
      compile( scalazCore ) ++
      compile( shapeless ) ++
      compile( akkaActor ) ++
      test( specs2 ) ++
      test( specs2Extra )
    )

  lazy val akka = Project( "peds-akka", file( "akka" ) )
    .dependsOn( commons )
    .settings( moduleSettings: _* )
    .settings( libraryDependencies ++=
      compile( config ) ++
      compile( akkaActor ) ++
      compile( akkaContrib ) ++
      compile( akkaPersistence ) ++
      compile( akkaSlf4j ) ++
      test( akkaTestKit ) ++
      test( scalatest ) 
    )

  lazy val archetype = Project( "peds-archetype", file( "archetype" ) )
    .dependsOn( commons )
    .settings( moduleSettings: _* )
    .settings( libraryDependencies ++=
      compile( config ) ++
      compile( eaio ) ++
      compile( logbackclassic ) ++
      compile( json4sJackson ) ++
      compile( scalalogging ) ++
      compile( joda ) ++
      compile( jodaConvert ) ++
      compile( scalaTime ) ++
      compile( shapeless ) ++
      test( specs2 )
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

  lazy val spray = Project( "peds-spray", file( "spray" ) )
    .dependsOn( commons )
    .settings( moduleSettings: _* )
    .settings( libraryDependencies ++=
      compile( config ) ++
      compile( akkaActor ) ++
      compile ( akkaSlf4j ) ++
      compile( sprayCan ) ++
      compile( sprayRouting ) ++
      compile( json4sJackson ) ++
      test( specs2 ) 
    )

}
