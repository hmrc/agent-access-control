import sbt.*

object AppDependencies {
  private val bootstrapVer = "10.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVer,
    "uk.gov.hmrc" %% "domain-play-30"            % "11.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"    %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "org.mockito"    %% "mockito-scala-scalatest" % "2.0.0"    % Test,
    "org.scalamock"  %% "scalamock"               % "7.4.1"      % Test,
    "org.scalacheck" %% "scalacheck"              % "1.18.1"     % Test,
  )
}
