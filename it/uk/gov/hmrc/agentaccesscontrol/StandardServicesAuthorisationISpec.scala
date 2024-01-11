package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.helpers.{MetricTestSupportServerPerTest, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, MtdItId, PptRef, Urn, Utr, Vrn}

class StandardServicesAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest with StandardAuthBehaviours {
  "MTD IT" should {
    behave like standardAuthBehaviour("mtd-it-auth", MtdItId("12345677890"), "ITSA")
  }
  "VAT" should {
    behave like standardAuthBehaviour("mtd-vat-auth", Vrn("12345677890"), "VATC")
  }
  "CGT" should {
    behave like standardAuthBehaviour("cgt-auth", CgtRef("XMCGTP123456789"), "CGT")
  }
  "PPT" should {
    behave like standardAuthBehaviour("ppt-auth", PptRef("XHPPT0006633194"), "PPT")
  }
  "Trust" should {
    behave like standardAuthBehaviour("trust-auth", Utr("0123456789"), "TRS")
  }
  "Non-taxable trust" should {
    behave like standardAuthBehaviour("trust-auth", Urn("XXTRUST12345678"), "TRS")
  }
}
