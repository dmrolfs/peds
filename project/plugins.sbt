// see: https://github.com/sbt/sbt-git#known-issues
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.30"

logLevel := Level.Warn

addSbtPlugin( "com.eed3si9n" % "sbt-buildinfo" % "0.9.0" )

addSbtPlugin("com.eed3si9n"                      % "sbt-assembly"           % "0.14.10")

addSbtPlugin("org.foundweekends"                 % "sbt-bintray"            % "0.5.5")
//addSbtPlugin("me.lessis"                         % "bintray-sbt"            % "0.3.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.7")
addSbtPlugin("org.scalastyle"                    %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"       % "2.1.0")
//addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"       % "1.1.0")

addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"            % "0.5.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-git"                % "1.0.0")
addSbtPlugin("net.virtual-void"                  % "sbt-dependency-graph"   % "0.9.2")
addSbtPlugin("io.get-coursier"                   % "sbt-coursier"           % "1.0.3")

addSbtPlugin("org.scoverage"                     % "sbt-scoverage"          % "1.6.1")
//addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.1.0")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

//addSbtPlugin("com.github.gseitz"                 % "sbt-release"            % "1.0.7")
//addSbtPlugin("com.lucidchart"                    % "sbt-scalafmt"           % "1.15")
// addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")
