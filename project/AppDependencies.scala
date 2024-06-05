import sbt.*

object AppDependencies {
  private val bootstrapVer = "8.6.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVer,
    "uk.gov.hmrc" %% "agent-mtd-identifiers" % "1.15.0",
    "uk.gov.hmrc" %% "play-allowlist-filter" % "1.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVer % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.31" % Test,
    "org.scalamock" %% "scalamock" % "6.0.0" % Test
  )
}
