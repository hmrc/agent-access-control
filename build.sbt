import CodeCoverageSettings.scoverageSettings
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.7.4"

val scalaCOptions = Seq(
  "-Werror",
  "-feature",
  "-Wsafe-init",
  "-Wvalue-discard",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=routes/.*:s"  // silence warnings from routes files
)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    name := "agent-access-control",
    organization := "uk.gov.hmrc",
    scalacOptions ++= scalaCOptions,
    PlayKeys.playDefaultPort := 9431,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Compile / scalacOptions := (Compile / scalacOptions).value.filterNot(Set("-deprecation", "-unchecked", "-encoding", "UTF-8", "utf8")).distinct,
    Test / scalacOptions := (Test / scalacOptions).value.filterNot(Set("-deprecation", "-unchecked", "-encoding", "UTF-8", "utf8")).distinct,
    Test / logBuffered := false
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    scalacOptions ++= scalaCOptions,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Compile / scalacOptions := (Compile / scalacOptions).value.filterNot(Set("-deprecation", "-unchecked", "-encoding", "UTF-8", "utf8")).distinct,
    Test / scalacOptions := (Test / scalacOptions).value.filterNot(Set("-deprecation", "-unchecked", "-encoding", "UTF-8", "utf8")).distinct,
    Test / logBuffered := false
  )
