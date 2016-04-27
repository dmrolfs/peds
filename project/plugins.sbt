resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.1.0")

addSbtPlugin( "com.eed3si9n" % "sbt-assembly" % "0.14.3" )

addSbtPlugin("com.sksamuel.sbt-versions" % "sbt-versions" % "0.2.0")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")