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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.CbcId
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.CgtRef
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.MtdItId
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.PptRef
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.Urn
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.Utr
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.Vrn
import uk.gov.hmrc.agentaccesscontrol.models.AccessResponse
import uk.gov.hmrc.agentaccesscontrol.models.Arn
import uk.gov.hmrc.agentaccesscontrol.models.AuthDetails
import uk.gov.hmrc.agentaccesscontrol.models.Service
import uk.gov.hmrc.agentaccesscontrol.models.Service.CapitalGains
import uk.gov.hmrc.agentaccesscontrol.models.SuspensionDetails
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

class ESAuthorisationServiceSpec extends UnitSpec {

  trait Setup {
    protected val mockAuditService: AuditService = mock[AuditService]
    protected val mockRelationshipsConnector: RelationshipsConnector =
      mock[RelationshipsConnector]
    protected val mockDesAgentClientApiConnector: DesAgentClientApiConnector =
      mock[DesAgentClientApiConnector]
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector =
      mock[AgentAssuranceConnector]
    protected val mockAgentPermissionsConnector: AgentPermissionsConnector =
      mock[AgentPermissionsConnector]
    protected val mockAppConfig: AppConfig = mock[AppConfig]

    object TestService
        extends ESAuthorisationService(
          mockRelationshipsConnector,
          mockDesAgentClientApiConnector,
          mockAgentAssuranceConnector,
          mockAgentPermissionsConnector,
          mockAuditService,
          mockAppConfig
        )
  }

  private val agentCode: AgentCode         = AgentCode("agentCode")
  private val arn: Arn                     = Arn("arn")
  private val saAgentRef: SaAgentReference = SaAgentReference("ABC456")
  private val mtdAuthDetails: AuthDetails =
    AuthDetails(None, Some(arn), "ggId", Some("Agent"), Some(User))
  private val nonMtdAuthDetails: AuthDetails =
    AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(User))
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent/arn/client/utr")

  private val templateTestDataSets: Seq[(Service, TaxIdentifier, String, String)] = Seq(
    (Service.MtdIt, MtdItId("clientId"), "ITSA", Service.MtdIt.id),
    (Service.MtdItSupp, MtdItId("clientId"), "ITSA", Service.MtdIt.id),
    (Service.Vat, Vrn("vrn"), "ALL", Service.Vat.id),
    (Service.Trust, Utr("utr"), "TRS", "HMRC-TERS"),
    (Service.TrustNT, Urn("urn"), "TRS", "HMRC-TERS"),
    (Service.CapitalGains, CgtRef("XMCGTP123456789"), "CGT", Service.CapitalGains.id),
    (Service.Ppt, PptRef("pptRef"), "AGSV", Service.Ppt.id),
    (Service.Cbc, CbcId("pptRef"), "CBC", "HMRC-CBC"),
    (Service.CbcNonUk, CbcId("pptRef"), "CBC", "HMRC-CBC")
  )

  templateTestDataSets.foreach(testData =>
    s"authorise ${testData._1.id}" should {
      "allow access for agent with a client relationship" in new Setup {
        mockAuditService
          .sendAuditEvent(
            *[AgentAccessControlEvent],
            *[String],
            *[AgentCode],
            *[String],
            *[TaxIdentifier],
            *[Seq[(String, Any)]]
          )
          .returns(Future.successful(Success))
        mockAgentPermissionsConnector
          .granularPermissionsOptinRecordExists(arn)
          .returns(Future.successful(true))
        mockAgentPermissionsConnector
          .getTaxServiceGroups(arn, testData._4)
          .returns(Future.successful(None))
        mockRelationshipsConnector
          .relationshipExists(arn, None, testData._2, testData._1)
          .returns(Future.successful(true))
        mockRelationshipsConnector
          .relationshipExists(arn, Some(mtdAuthDetails.ggCredentialId), testData._2, testData._1)
          .returns(Future.successful(true))
        mockAgentAssuranceConnector.getSuspensionDetails
          .returns(Future.successful(SuspensionDetails.notSuspended))

        val result: AccessResponse =
          await(TestService.authoriseStandardService(agentCode, testData._2, testData._1, mtdAuthDetails))

        result mustBe AccessResponse.Authorised
      }

      "deny access for a non-mtd agent" in new Setup {
        mockAuditService
          .sendAuditEvent(
            *[AgentAccessControlEvent],
            *[String],
            *[AgentCode],
            *[String],
            *[TaxIdentifier],
            *[Seq[(String, Any)]]
          )
          .returns(Future.successful(Success))

        val result: AccessResponse =
          await(TestService.authoriseStandardService(agentCode, testData._2, testData._1, nonMtdAuthDetails))

        result mustBe AccessResponse.NoRelationship
      }

      "deny access for a mtd agent without a client relationship" in new Setup {
        mockAgentPermissionsConnector
          .granularPermissionsOptinRecordExists(arn)
          .returns(Future.successful(true))
        mockAuditService
          .sendAuditEvent(
            *[AgentAccessControlEvent],
            *[String],
            *[AgentCode],
            *[String],
            *[TaxIdentifier],
            *[Seq[(String, Any)]]
          )
          .returns(Future.successful(Success))
        mockRelationshipsConnector
          .relationshipExists(arn, None, testData._2, testData._1)
          .returns(Future.successful(false))
        mockAgentAssuranceConnector.getSuspensionDetails
          .returns(Future.successful(SuspensionDetails.notSuspended))

        val result: AccessResponse =
          await(TestService.authoriseStandardService(agentCode, testData._2, testData._1, mtdAuthDetails))

        result mustBe AccessResponse.NoRelationship
      }

      "deny access for a mtd agent with a client relationship but agent user is unassigned (and access groups turned on)" in new Setup {
        mockAuditService
          .sendAuditEvent(
            *[AgentAccessControlEvent],
            *[String],
            *[AgentCode],
            *[String],
            *[TaxIdentifier],
            *[Seq[(String, Any)]]
          )
          .returns(Future.successful(Success))
        mockRelationshipsConnector
          .relationshipExists(arn, None, testData._2, testData._1)
          .returns(Future.successful(true))
        mockRelationshipsConnector
          .relationshipExists(arn, Some(mtdAuthDetails.ggCredentialId), testData._2, testData._1)
          .returns(Future.successful(false))
        mockAgentAssuranceConnector.getSuspensionDetails
          .returns(Future.successful(SuspensionDetails.notSuspended))
        mockAppConfig.enableGranularPermissions.returns(true)
        mockAgentPermissionsConnector
          .granularPermissionsOptinRecordExists(arn)
          .returns(Future.successful(true))
        mockAgentPermissionsConnector
          .getTaxServiceGroups(arn, testData._4)
          .returns(Future.successful(None))

        val result: AccessResponse =
          await(TestService.authoriseStandardService(agentCode, testData._2, testData._1, mtdAuthDetails))

        result mustBe AccessResponse.NoAssignment
      }

      "handle suspended agents" in new Setup {
        mockAgentAssuranceConnector.getSuspensionDetails
          .returns(
            Future.successful(
              SuspensionDetails(suspensionStatus = true, Some(Set(testData._3)))
            )
          )

        val result: AccessResponse =
          await(TestService.authoriseStandardService(agentCode, testData._2, testData._1, mtdAuthDetails))

        result mustBe AccessResponse.AgentSuspended
      }
    }
  )

  "granular permissions logic" should {
    "when GP enabled and opted in, specify user to check in relationship service call" in new Setup {
      private val cgtRef = CgtRef("XMCGTP123456789")
      mockAuditService
        .sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]
        )
        .returns(Future.successful(Success))
      mockAgentAssuranceConnector.getSuspensionDetails
        .returns(Future.successful(SuspensionDetails.notSuspended))
      mockAgentPermissionsConnector
        .granularPermissionsOptinRecordExists(arn)
        .returns(
          Future
            .successful(true)
        )
      mockAgentPermissionsConnector
        .getTaxServiceGroups(arn, CapitalGains.id)
        .returns(
          Future
            .successful(None)
        )
      mockRelationshipsConnector
        .relationshipExists(arn, Some("ggId"), cgtRef, Service.CapitalGains)
        .returns(
          Future
            .successful(true)
        )
      mockRelationshipsConnector
        .relationshipExists(arn, None, cgtRef, Service.CapitalGains)
        .returns(
          Future
            .successful(true)
        )

      await(TestService.authoriseStandardService(agentCode, cgtRef, Service.CapitalGains, mtdAuthDetails))
    }

    "when GP outed out, do NOT specify user to check in relationship service call" in new Setup {
      private val cgtRef = CgtRef("XMCGTP123456789")
      mockAuditService
        .sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]
        )
        .returns(Future.successful(Success))
      mockAgentAssuranceConnector.getSuspensionDetails
        .returns(Future.successful(SuspensionDetails.notSuspended))
      mockAgentPermissionsConnector
        .granularPermissionsOptinRecordExists(arn)
        .returns(
          Future
            .successful(false)
        )
      mockRelationshipsConnector
        .relationshipExists(arn, None, cgtRef, Service.CapitalGains)
        .returns(
          Future
            .successful(true)
        )

      await(TestService.authoriseStandardService(agentCode, cgtRef, Service.CapitalGains, mtdAuthDetails))
    }
  }
}
