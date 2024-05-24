import sbt._

object AppDependencies {
  private val bootstrapVer = "8.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30"  % bootstrapVer,
    "uk.gov.hmrc" %% "agent-mtd-identifiers"      % "1.15.0",
    "uk.gov.hmrc" %% "play-allowlist-filter"      % "1.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "org.scalatest"          %% "scalatest"               % "3.2.18"      % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1"      % Test,
    "org.mockito"            %% "mockito-scala"           % "1.17.31"    % Test,
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.31"    % Test,
    "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
    "com.github.tomakehurst"  % "wiremock-jre8"           % "3.0.1"     % Test,
    "org.scalamock"          %% "scalamock"               % "6.0.0"      % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8"     % Test
  )
}
