import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28"  % "7.12.0",
    "uk.gov.hmrc" %% "play-allowlist-filter"      % "1.1.0",
    "uk.gov.hmrc" %% "agent-mtd-identifiers"      % "1.2.0",
    "uk.gov.hmrc" %% "agent-kenshoo-monitoring"   % "4.8.0-play-28"
  )

  val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"         % "test, it",
    "org.scalatestplus"      %% "mockito-3-12"       % "3.2.10.0"      % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.26.1"        % "test, it",
    "org.scalamock"          %% "scalamock"          % "4.4.0"         % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.35.10"       % "test, it"
  )
}
