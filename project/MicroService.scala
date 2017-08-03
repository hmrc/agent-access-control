import play.sbt.PlayImport.PlayKeys
import play.sbt.PlayImport.PlayKeys._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._


trait MicroService {

  import org.scalastyle.sbt.ScalastylePlugin.scalastyleConfig
  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import play.sbt.routes.RoutesKeys.routesImport


  import TestPhases._

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      // Semicolon-separated list of regexs matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*""",
      ScoverageKeys.coverageMinimum := 80,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true,
      parallelExecution in Test := false
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala) ++ plugins : _*)
    .settings(PlayKeys.playDefaultPort := 9431)
    .settings(playSettings ++ scoverageSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(routesImport ++= Seq("uk.gov.hmrc.agentaccesscontrol.binders.PathBinders._"))
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.8",
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      parallelExecution in Test := false,
      fork in Test := false,
      (scalastyleConfig in Test) := baseDirectory.value / "scalastyle-test-config.xml"
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
