
object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "uk.gov.hmrc.BuildInfo",
    "Reverse.*",
    "app.assets.*",
    ".*Routes.*",
    ".*Filters?",
    "MicroserviceAuditConnector",
    "GraphiteStartUp",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*"
  )


  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 80.00,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }
}

