import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  "uk.gov.hmrc" %% "bootstrap-backend-play-27"  % "3.4.0",
  "uk.gov.hmrc" %% "play-whitelist-filter"      % "3.4.0-play-27",
  "uk.gov.hmrc" %% "domain"                     % "5.10.0-play-27",
  "uk.gov.hmrc" %% "agent-mtd-identifiers"      % "0.23.0-play-27",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring"   % "4.4.0"
)

def testDeps(scope: String) = Seq(
  "uk.gov.hmrc"            %% "hmrctest"           % "3.8.0-play-26" % scope,
  "org.scalatest"          %% "scalatest"          % "3.0.8"         % scope,
  "org.mockito"             % "mockito-core"       % "2.27.0"        % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"         % scope,
  "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.2"        % scope,
  "org.scalamock"          %% "scalamock"          % "4.4.0"         % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-access-control",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.10",
    majorVersion := 0,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-value-discard",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes"),
    PlayKeys.playDefaultPort := 9431,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    routesImport += "uk.gov.hmrc.agentaccesscontrol.binders.PathBinders._",
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true
  )
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    scalafmtOnCompile in IntegrationTest := true
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

inConfig(IntegrationTest)(scalafmtCoreSettings)
