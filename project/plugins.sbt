resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "0.99.7.1")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "0.99.0")

addSbtPlugin( "com.eed3si9n" % "sbt-assembly" % "0.11.2" )

addSbtPlugin("com.sksamuel.sbt-versions" % "sbt-versions" % "0.2.0")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
