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
    Test / parallelExecution := false
  )
}

lazy val compileDeps = Seq(
  "uk.gov.hmrc" %% "bootstrap-backend-play-28"  % "7.12.0",
  "uk.gov.hmrc" %% "play-allowlist-filter"      % "1.1.0",
  "uk.gov.hmrc" %% "agent-mtd-identifiers"      % "0.51.0-play-28",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring"   % "4.8.0-play-28"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"         % scope,
  "org.scalatestplus"      %% "mockito-3-12"       % "3.2.10.0"      % scope,
  "com.github.tomakehurst"  % "wiremock-jre8"      % "2.26.1"        % scope,
  "org.scalamock"          %% "scalamock"          % "4.4.0"         % scope,
  "com.vladsch.flexmark"    % "flexmark-all"       % "0.35.10"       % scope
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
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    routesImport += "uk.gov.hmrc.agentaccesscontrol.binders.PathBinders._",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

inConfig(IntegrationTest)(scalafmtCoreSettings)
