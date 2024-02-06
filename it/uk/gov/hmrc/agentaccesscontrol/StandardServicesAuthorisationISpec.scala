package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.helpers.MetricTestSupportServerPerTest
import uk.gov.hmrc.agentaccesscontrol.helpers.WireMockWithOneServerPerTestISpec
import uk.gov.hmrc.agentmtdidentifiers.model.CgtRef
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentmtdidentifiers.model.PptRef
import uk.gov.hmrc.agentmtdidentifiers.model.Urn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn

class StandardServicesAuthorisationISpec
    extends WireMockWithOneServerPerTestISpec
    with MetricTestSupportServerPerTest
    with StandardAuthBehaviours {
  "MTD IT" should {
    behave.like(standardAuthBehaviour("mtd-it-auth", MtdItId("12345677890"), "ITSA"))
  }
  "VAT" should {
    behave.like(standardAuthBehaviour("mtd-vat-auth", Vrn("12345677890"), "VATC"))
  }
  "CGT" should {
    behave.like(standardAuthBehaviour("cgt-auth", CgtRef("XMCGTP123456789"), "CGT"))
  }
  "PPT" should {
    behave.like(standardAuthBehaviour("ppt-auth", PptRef("XHPPT0006633194"), "PPT"))
  }
  "Trust" should {
    behave.like(standardAuthBehaviour("trust-auth", Utr("0123456789"), "TRS"))
  }
  "Non-taxable trust" should {
    behave.like(standardAuthBehaviour("trust-auth", Urn("XXTRUST12345678"), "TRS"))
  }
}
