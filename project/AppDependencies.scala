import sbt.*

object AppDependencies {
  private val bootstrapVer = "9.13.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVer,
    "uk.gov.hmrc" %% "agent-mtd-identifiers" % "2.2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVer % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.31" % Test,
    "org.scalamock" %% "scalamock" % "6.0.0" % Test
  )
}
