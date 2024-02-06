resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)


addSbtPlugin("com.typesafe.play" % "sbt-plugin"           % "2.8.21")

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"       % "3.20.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"   % "2.4.0")

addSbtPlugin("org.scalameta"     % "sbt-scalafmt"         % "2.5.0")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"          % "0.3.4")  // provides sbt command "dependencyUpdates"
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph" % "0.9.2")  // provides sbt command "dependencyTree"
addSbtPlugin("org.scoverage"     % "sbt-scoverage"        % "2.0.6")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin"% "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))

//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
