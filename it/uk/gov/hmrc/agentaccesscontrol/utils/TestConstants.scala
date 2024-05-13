package uk.gov.hmrc.agentaccesscontrol.utils

import java.time.LocalDateTime
import java.util.UUID

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentmtdidentifiers.model.CgtRef
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentmtdidentifiers.model.PlrId
import uk.gov.hmrc.agentmtdidentifiers.model.PptRef
import uk.gov.hmrc.agentmtdidentifiers.model.Urn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.agents.accessgroups.AgentUser
import uk.gov.hmrc.agents.accessgroups.Client
import uk.gov.hmrc.agents.accessgroups.TaxGroup
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.SaUtr

object TestConstants {

  val testAgentCode: AgentCode               = AgentCode("A11112222A")
  val testArn: Arn                           = Arn("01234567890")
  val testCgtRef: CgtRef                     = CgtRef("XMCGTP123456789")
  val testProviderId: String                 = "12345-credId"
  val testNino: Nino                         = Nino("AE123456C")
  val testUtr: Utr                           = Utr("5066836985")
  val testUrn: Urn                           = Urn("XATRUST06683698")
  val testMtdItId: MtdItId                   = MtdItId("C1111C")
  val testVrn: Vrn                           = Vrn("101747641")
  val testPptRef: PptRef                     = PptRef("XHPPT0006633194")
  val testCbcId: CbcId                       = CbcId("XHCBC0006633194")
  val testPlrId: PlrId                       = PlrId("XDPLR6210917659")
  val testSaUtr: SaUtr                       = SaUtr("1234567890")
  val testEmpRef: EmpRef                     = EmpRef("123", "4567890")
  val testSaAgentReference: SaAgentReference = SaAgentReference("enrol-123")

  val testCgtEnrolmentKey: String      = "HMRC-CGT-PD"
  private val trustTaxGroupKey: String = "HMRC-TERS"
  val testTersEnrolmentKey: String     = "HMRC-TERS-ORG"
  val testTersntEnrolmentKey: String   = "HMRC-TERSNT-ORG"
  val testMtdVatEnrolmentKey: String   = "HMRC-MTD-VAT"

  val testCgtClient: Client = Client(s"HMRC-CGT-PD~CGTPDRef~${testCgtRef.value}", "Zoe Client")

  def testCgtTaxGroup(providerId: String = testProviderId, excludedClients: Set[Client] = Set.empty): TaxGroup =
    TaxGroup(
      id = UUID.randomUUID(),
      arn = testArn,
      groupName = s"$testCgtEnrolmentKey-group",
      created = LocalDateTime.now,
      lastUpdated = LocalDateTime.now,
      createdBy = AgentUser("testId", "testName"),
      lastUpdatedBy = AgentUser("testId", "testName"),
      teamMembers = Set(AgentUser(providerId, "Test Agent")),
      service = testCgtEnrolmentKey,
      automaticUpdates = false,
      excludedClients = excludedClients
    )

  val testTrustTaxGroup: TaxGroup =
    TaxGroup(
      id = UUID.randomUUID(),
      arn = testArn,
      groupName = s"$trustTaxGroupKey-group",
      created = LocalDateTime.now,
      lastUpdated = LocalDateTime.now,
      createdBy = AgentUser("testId", "testName"),
      lastUpdatedBy = AgentUser("testId", "testName"),
      teamMembers = Set(AgentUser(testProviderId, "Test Agent")),
      service = trustTaxGroupKey, // tax service groups use the nonstandard key "HMRC-TERS" to mean either type of trust
      automaticUpdates = false,
      excludedClients = Set.empty
    )

  val testMtdVatTaxGroup: TaxGroup =
    TaxGroup(
      id = UUID.randomUUID(),
      arn = testArn,
      groupName = s"$testMtdVatEnrolmentKey-group",
      created = LocalDateTime.now,
      lastUpdated = LocalDateTime.now,
      createdBy = AgentUser("testId", "testName"),
      lastUpdatedBy = AgentUser("testId", "testName"),
      teamMembers = Set(AgentUser(testProviderId, "Test Agent")),
      service = testMtdVatEnrolmentKey,
      automaticUpdates = false,
      excludedClients = Set.empty
    )

}
