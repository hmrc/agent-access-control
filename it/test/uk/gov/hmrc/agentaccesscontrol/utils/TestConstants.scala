/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentaccesscontrol.utils

import java.time.LocalDateTime
import java.util.UUID

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentmtdidentifiers.model.CgtRef
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentmtdidentifiers.model.PlrId
import uk.gov.hmrc.agentmtdidentifiers.model.PptRef
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCCGTPD
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCMTDIT
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCMTDITSUPP
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCMTDVAT
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCPILLAR2ORG
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCPPTORG
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCTERSNTORG
import uk.gov.hmrc.agentmtdidentifiers.model.Service.HMRCTERSORG
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
import uk.gov.hmrc.domain.TaxIdentifier

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

  val testCgtEnrolmentKey: String       = "HMRC-CGT-PD"
  private val trustTaxGroupKey: String  = "HMRC-TERS"
  val testTersEnrolmentKey: String      = "HMRC-TERS-ORG"
  val testTersntEnrolmentKey: String    = "HMRC-TERSNT-ORG"
  val testMtdVatEnrolmentKey: String    = "HMRC-MTD-VAT"
  val testMtdItEnrolmentKey: String     = "HMRC-MTD-IT"
  val testMtdItSuppEnrolmentKey: String = "HMRC-MTD-IT-SUPP"
  val testPptEnrolmentKey: String       = "HMRC-PPT-ORG"
  val testPillar2EnrolmentKey: String   = "HMRC-PILLAR2-ORG"
  // TODO handle CBC which has an additional request to get identifiers

  val testCgtClient: Client = Client(s"HMRC-CGT-PD~CGTPDRef~${testCgtRef.value}", "Zoe Client")

  private val mtdItAuth   = "mtd-it-auth"
  private val mtdItRegime = "ITSA"
  private val mtdItIdUri  = s"/$mtdItAuth/agent/${testAgentCode.value}/client/${testMtdItId.value}"

  private val mtdItSuppAuth   = "mtd-it-auth-supp"
  private val mtdItSuppRegime = "ITSA"
  private val mtdItSuppUri    = s"/$mtdItSuppAuth/agent/${testAgentCode.value}/client/${testMtdItId.value}"

  private val mtdVatAuth   = "mtd-vat-auth"
  private val mtdVatRegime = "VATC"
  private val mtdVatUri    = s"/$mtdVatAuth/agent/${testAgentCode.value}/client/${testVrn.value}"

  private val cgtAuth   = "cgt-auth"
  private val cgtRegime = "CGT"
  private val cgtUri    = s"/$cgtAuth/agent/${testAgentCode.value}/client/${testCgtRef.value}"

  private val pptAuth   = "ppt-auth"
  private val pptRegime = "PPT"
  private val pptUri    = s"/$pptAuth/agent/${testAgentCode.value}/client/${testPptRef.value}"

  private val trustAuth          = "trust-auth"
  private val trustRegime        = "TRS"
  private val trustUri           = s"/$trustAuth/agent/${testAgentCode.value}/client/${testUtr.value}"
  private val nonTaxableTrustUri = s"/$trustAuth/agent/${testAgentCode.value}/client/${testUrn.value}"

  private val pillar2Auth   = "pillar2-auth"
  private val pillar2Regime = "PLR"
  private val pillar2Uri    = s"/$pillar2Auth/agent/${testAgentCode.value}/client/${testPlrId.value}"

  case object Vat extends StandardServiceAuthorisationRequest {
    val uri: String                  = mtdVatUri
    val authRule: String             = mtdVatAuth
    val regime: String               = mtdVatRegime
    val service: String              = HMRCMTDVAT
    val taxIdentifierIdType: String  = "VRN"
    val taxIdentifier: TaxIdentifier = testVrn
    val taxGroup: TaxGroup           = testTaxServiceGroup(testMtdVatEnrolmentKey)
  }

  case object ItsaMain extends StandardServiceAuthorisationRequest {
    val uri: String                  = mtdItIdUri
    val authRule: String             = mtdItAuth
    val regime: String               = mtdItRegime
    val service: String              = HMRCMTDIT
    val taxIdentifierIdType: String  = "MTDITID"
    val taxIdentifier: TaxIdentifier = testMtdItId
    val taxGroup: TaxGroup           = testTaxServiceGroup(testMtdItEnrolmentKey)
  }

  case object ItsaSupp extends StandardServiceAuthorisationRequest {
    val uri: String                  = mtdItSuppUri
    val authRule: String             = mtdItSuppAuth
    val regime: String               = mtdItSuppRegime
    val service: String              = HMRCMTDITSUPP
    val taxIdentifierIdType: String  = "MTDITID"
    val taxIdentifier: TaxIdentifier = testMtdItId
    val taxGroup: TaxGroup           = testTaxServiceGroup(testMtdItEnrolmentKey)
  }

  case object CgtPd extends StandardServiceAuthorisationRequest {
    val uri: String                  = cgtUri
    val authRule: String             = cgtAuth
    val regime: String               = cgtRegime
    val service: String              = HMRCCGTPD
    val taxIdentifierIdType: String  = "CGTPDRef"
    val taxIdentifier: TaxIdentifier = testCgtRef
    val taxGroup: TaxGroup           = testTaxServiceGroup(testCgtEnrolmentKey)
  }

  case object Trust extends StandardServiceAuthorisationRequest {
    val uri: String                  = trustUri
    val authRule: String             = trustAuth
    val regime: String               = trustRegime
    val service: String              = HMRCTERSORG
    val taxIdentifierIdType: String  = "SAUTR"
    val taxIdentifier: TaxIdentifier = testUtr
    val taxGroup: TaxGroup           = testTaxServiceGroup(trustTaxGroupKey)
  }

  case object TrustNT extends StandardServiceAuthorisationRequest {
    val uri: String                  = nonTaxableTrustUri
    val authRule: String             = trustAuth
    val regime: String               = trustRegime
    val service: String              = HMRCTERSNTORG
    val taxIdentifierIdType: String  = "URN"
    val taxIdentifier: TaxIdentifier = testUrn
    val taxGroup: TaxGroup           = testTaxServiceGroup(trustTaxGroupKey)
  }

  case object PPT extends StandardServiceAuthorisationRequest {
    val uri: String                  = pptUri
    val authRule: String             = pptAuth
    val regime: String               = pptRegime
    val service: String              = HMRCPPTORG
    val taxIdentifierIdType: String  = "EtmpRegistrationNumber"
    val taxIdentifier: TaxIdentifier = testPptRef
    val taxGroup: TaxGroup           = testTaxServiceGroup(testPptEnrolmentKey)
  }

  case object Pillar2 extends StandardServiceAuthorisationRequest {
    val uri: String                  = pillar2Uri
    val authRule: String             = pillar2Auth
    val regime: String               = pillar2Regime
    val service: String              = HMRCPILLAR2ORG
    val taxIdentifierIdType: String  = "PLRID"
    val taxIdentifier: TaxIdentifier = testPlrId
    val taxGroup: TaxGroup           = testTaxServiceGroup(testPillar2EnrolmentKey)
  }

  def testTaxServiceGroup(taxGroupService: String): TaxGroup = TaxGroup(
    id = UUID.randomUUID(),
    arn = testArn,
    groupName = s"$taxGroupService-group",
    created = LocalDateTime.now,
    lastUpdated = LocalDateTime.now,
    createdBy = AgentUser("testId", "testName"),
    lastUpdatedBy = AgentUser("testId", "testName"),
    teamMembers = Set(AgentUser(testProviderId, "Test Agent")),
    service = taxGroupService,
    automaticUpdates = false,
    excludedClients = Set.empty
  )

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
