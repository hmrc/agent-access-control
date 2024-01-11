/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.services

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitTest
import uk.gov.hmrc.agentaccesscontrol.mocks.config.MockAppConfig.mockAppConfig
import uk.gov.hmrc.agentaccesscontrol.mocks.connectors.{
  MockACAConnector,
  MockAgentPermissionsConnector,
  MockDesAgentClientApiConnector,
  MockRelationshipsConnector
}
import uk.gov.hmrc.agentaccesscontrol.mocks.services.MockAuditService
import uk.gov.hmrc.agentaccesscontrol.models.{AccessResponse, AuthDetails}
import uk.gov.hmrc.agentmtdidentifiers.model.Service.CapitalGains

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ESAuthorisationServiceSpec
    extends UnitTest
    with MockAuditService
    with MockRelationshipsConnector
    with MockDesAgentClientApiConnector
    with MockACAConnector
    with MockAgentPermissionsConnector {

  private val esAuthService =
    new ESAuthorisationService(mockRelationshipsConnector,
                               mockDesAgentClientApiConnector,
                               mockACAConnector,
                               mockAgentPermissionsConnector,
                               mockAuditService,
                               mockAppConfig)

  private val agentCode: AgentCode = AgentCode("agentCode")
  private val arn: Arn = Arn("arn")
  private val saAgentRef: SaAgentReference = SaAgentReference("ABC456")
  private val mtdAuthDetails: AuthDetails =
    AuthDetails(None, Some(arn), "ggId", Some("Agent"), Some(User))
  private val nonMtdAuthDetails: AuthDetails =
    AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(User))
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent/arn/client/utr")

  private val templateTestDataSets
    : Seq[(Service, TaxIdentifier, String, String)] = Seq(
    (Service.MtdIt, MtdItId("clientId"), "ITSA", Service.MtdIt.id),
    (Service.Vat, Vrn("vrn"), "ALL", Service.Vat.id),
    (Service.Trust, Utr("utr"), "TRS", "HMRC-TERS"),
    (Service.TrustNT, Urn("urn"), "TRS", "HMRC-TERS"),
    (Service.CapitalGains,
     CgtRef("XMCGTP123456789"),
     "CGT",
     Service.CapitalGains.id),
    (Service.Ppt, PptRef("pptRef"), "AGSV", Service.Ppt.id),
    (Service.Cbc, CbcId("pptRef"), "CBC", "HMRC-CBC"),
    (Service.CbcNonUk, CbcId("pptRef"), "CBC", "HMRC-CBC")
  )

  templateTestDataSets.foreach(testData =>
    s"authorise ${testData._1.id}" should {
      "allow access for agent with a client relationship" in {
        mockSendAuditEvent
        mockGranularPermissionsOptinRecordExists(arn, result = true)
        mockGetTaxServiceGroups(arn, testData._4, None)
        mockRelationshipExists(arn, None, testData._2, Future.successful(true))
        mockRelationshipExists(arn,
                               Some(mtdAuthDetails.ggCredentialId),
                               testData._2,
                               Future.successful(true))
        mockGetSuspensionDetails(
          arn,
          Future.successful(SuspensionDetails.notSuspended))

        val result =
          await(
            esAuthService.authoriseStandardService(agentCode,
                                                   testData._2,
                                                   testData._1.id,
                                                   mtdAuthDetails))

        result shouldBe AccessResponse.Authorised
      }

      "deny access for a non-mtd agent" in {
        mockSendAuditEvent

        val result =
          await(
            esAuthService.authoriseStandardService(agentCode,
                                                   testData._2,
                                                   testData._1.id,
                                                   nonMtdAuthDetails))

        result shouldBe AccessResponse.NoRelationship
      }

      "deny access for a mtd agent without a client relationship" in {
        mockGranularPermissionsOptinRecordExists(arn, result = true)
        mockSendAuditEvent
        mockRelationshipExists(arn, None, testData._2, Future.successful(false))
        mockGetSuspensionDetails(
          arn,
          Future.successful(SuspensionDetails.notSuspended))

        val result =
          await(
            esAuthService.authoriseStandardService(agentCode,
                                                   testData._2,
                                                   testData._1.id,
                                                   mtdAuthDetails))

        result shouldBe AccessResponse.NoRelationship
      }

      "deny access for a mtd agent with a client relationship but agent user is unassigned (and access groups turned on)" in {
        mockSendAuditEvent
        mockRelationshipExists(arn, None, testData._2, Future.successful(true))
        mockRelationshipExists(arn,
                               Some(mtdAuthDetails.ggCredentialId),
                               testData._2,
                               Future.successful(false))
        mockGetSuspensionDetails(
          arn,
          Future.successful(SuspensionDetails.notSuspended))
        mockGranularPermissionsOptinRecordExists(arn, result = true)
        mockGetTaxServiceGroups(arn, testData._4, None)

        val result =
          await(
            esAuthService.authoriseStandardService(agentCode,
                                                   testData._2,
                                                   testData._1.id,
                                                   mtdAuthDetails))

        result shouldBe AccessResponse.NoAssignment
      }

      "handle suspended agents" in {
        mockGetSuspensionDetails(
          arn,
          Future.successful(
            SuspensionDetails(suspensionStatus = true, Some(Set(testData._3)))))

        val result =
          await(
            esAuthService.authoriseStandardService(agentCode,
                                                   testData._2,
                                                   testData._1.id,
                                                   mtdAuthDetails))

        result shouldBe AccessResponse.AgentSuspended
      }
  })

  "granular permissions logic" should {
    "when GP enabled and opted in, specify user to check in relationship service call" in {
      val cgtRef = CgtRef("XMCGTP123456789")
      mockSendAuditEvent
      mockGetSuspensionDetails(
        arn,
        Future.successful(SuspensionDetails.notSuspended))
      mockGranularPermissionsOptinRecordExists(arn, result = true)
      mockGetTaxServiceGroups(arn, CapitalGains.id, None)
      mockRelationshipExists(arn, Some("ggId"), cgtRef, Future.successful(true))
      mockRelationshipExists(arn, None, cgtRef, Future.successful(true))

      await(
        esAuthService.authoriseStandardService(agentCode,
                                               cgtRef,
                                               Service.CapitalGains.id,
                                               mtdAuthDetails))
    }

    "when GP outed out, do NOT specify user to check in relationship service call" in {
      val cgtRef = CgtRef("XMCGTP123456789")
      mockSendAuditEvent
      mockGetSuspensionDetails(
        arn,
        Future.successful(SuspensionDetails.notSuspended))
      mockGranularPermissionsOptinRecordExists(arn, result = false)
      mockRelationshipExists(arn, None, cgtRef, Future.successful(true))

      await(
        esAuthService.authoriseStandardService(agentCode,
                                               cgtRef,
                                               Service.CapitalGains.id,
                                               mtdAuthDetails))
    }
  }

}
