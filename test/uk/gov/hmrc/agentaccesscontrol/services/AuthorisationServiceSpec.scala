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
import play.api.test.Helpers.await
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlEvent,
  AuditService
}
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.{
  AfiRelationshipConnector,
  MappingConnector
}
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.agentaccesscontrol.models.{
  AccessResponse,
  AgentReferenceMapping,
  AgentReferenceMappings,
  AuthDetails
}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.{
  BadRequestException,
  HeaderCarrier,
  UpstreamErrorResponse
}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationServiceSpec extends UnitSpec {

  trait Setup {
    protected val mockDesAuthorisationService: DesAuthorisationService =
      mock[DesAuthorisationService]
    protected val mockEnrolmentStoreProxyAuthorisationService
      : EnrolmentStoreProxyAuthorisationService =
      mock[EnrolmentStoreProxyAuthorisationService]
    protected val mockAuditService: AuditService = mock[AuditService]
    protected val mockMappingConnector: MappingConnector =
      mock[MappingConnector]
    protected val mockAfiRelationshipConnector: AfiRelationshipConnector =
      mock[AfiRelationshipConnector]
    protected val mockAgentClientAuthorisationConnector
      : AgentClientAuthorisationConnector =
      mock[AgentClientAuthorisationConnector]

    object TestService
        extends AuthorisationService(
          mockDesAuthorisationService,
          mockEnrolmentStoreProxyAuthorisationService,
          mockAuditService,
          mockMappingConnector,
          mockAfiRelationshipConnector,
          mockAgentClientAuthorisationConnector
        )
  }

  private val agentCode = AgentCode("ABCDEF123456")
  private val saAgentRef = SaAgentReference("ABC456")
  private val saUtr = SaUtr("CLIENTSAUTR456")
  private val empRef = EmpRef("123", "01234567")
  private val nino: Nino = Nino("AA101010A")
  private val credId: String = "ggId"
  private val arn: Arn = Arn("arn")
  private val failedResponse = Future.failed(UpstreamErrorResponse("boom", 503))
  private val badRequestException =
    Future.failed(new BadRequestException("bad request"))

  private val mtdAuthDetails =
    AuthDetails(None, Some(Arn("arn")), credId, None, None)
  private val nonMtdAuthDetails =
    AuthDetails(Some(saAgentRef), None, credId, Some("Agent"), Some(User))

  private val afiAuthDetails = AuthDetails(None, Some(arn), credId, None, None)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext =
    concurrent.ExecutionContext.Implicits.global
  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
    "GET",
    s"/agent-access-control/sa-auth/agent/$agentCode/client/$saUtr")

  "AuthorisationService.isAuthorisedForSa" when {
    "nonMtdAgentAuthDetails provided" should {
      "return NoRelationship if user is not authorised for SA in ESP" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForSaInEnrolmentStoreProxy(credId, saUtr) returns Future
          .successful(false)

        await(TestService.isAuthorisedForSa(
          agentCode,
          saUtr,
          nonMtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return NoRelationship if user is authorised for SA in ESP, but not authorised for SA in CESA" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForSaInEnrolmentStoreProxy(credId, saUtr) returns Future
          .successful(true)
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns Future.successful(false)

        await(TestService.isAuthorisedForSa(
          agentCode,
          saUtr,
          nonMtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return Authorised if user is authorised in both ESP and CESA" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForSaInEnrolmentStoreProxy(credId, saUtr) returns Future
          .successful(true)
        mockDesAuthorisationService.isAuthorisedInCesa(agentCode,
                                                       saAgentRef,
                                                       saUtr) returns
          Future.successful(true)

        await(TestService.isAuthorisedForSa(
          agentCode,
          saUtr,
          nonMtdAuthDetails)) mustBe AccessResponse.Authorised
      }
      "return an Error if ESP throws an error" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForSaInEnrolmentStoreProxy(credId, saUtr) returns badRequestException

        intercept[BadRequestException] {
          await(
            TestService.isAuthorisedForSa(agentCode, saUtr, nonMtdAuthDetails))
        }
      }
      "return an Error if CESA throws an error" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForSaInEnrolmentStoreProxy(credId, saUtr) returns Future
          .successful(true)
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns badRequestException

        intercept[BadRequestException] {
          await(
            TestService.isAuthorisedForSa(agentCode, saUtr, nonMtdAuthDetails))
        }
      }
    }
    "mtdAgentAuthDetails provided" should {
      "return NoRelationship if no DelegatedAgentUserIds found in ESP" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)

        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns Future.successful(Set.empty)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return an exception if there is an error when calling ESP to GetDelegatedAgentUserIds" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns badRequestException

        intercept[BadRequestException] {
          await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails))
        }
      }
      "return NoRelationship if no agent mappings are found" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns Future
          .successful(AgentReferenceMappings(List.empty))
        //TODO no need to make this call if we got an empty list previously
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List.empty) returns Future.successful(Seq.empty)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return NoRelationship if no agent user ids are found in Agent Mappings or ESP" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef)) returns Future.successful(Seq.empty)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return an exception if there is an error when calling Agent Mappings" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns badRequestException

        intercept[BadRequestException] {
          await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails))
        }
      }
      "return an exception if there is an error when calling ESP GetAgentUserIds" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef)) returns badRequestException

        intercept[BadRequestException] {
          await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails))
        }
      }
      "return NoRelationship if userIds found, but do not match" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef)) returns
          Future.successful(Seq((saAgentRef, Set(AgentUserId("cred2")))))

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return NoRelationship if userIds found, but not authorised in CESA" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef)) returns
          Future.successful(Seq((saAgentRef, Set(AgentUserId("cred1")))))

        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns Future.successful(false)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
      "return Authorised if there is one agent user id and it is authorised in CESA" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef)) returns
          Future.successful(Seq((saAgentRef, Set(AgentUserId("cred1")))))
        mockDesAuthorisationService.isAuthorisedInCesa(agentCode,
                                                       saAgentRef,
                                                       saUtr) returns
          Future.successful(true)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.Authorised
      }
      "return Authorised if the first agent user id is not authorised, but the second is" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC"))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC"))) returns
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2"))))
          )
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns Future.successful(false)

        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          SaAgentReference("654ABC"),
          saUtr) returns Future.successful(true)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.Authorised
      }
      "return Authorised if the first agent user id gets an error from CESA, but the second gets an authorised response" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC"))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC"))) returns
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2"))))
          )
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns failedResponse

        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          SaAgentReference("654ABC"),
          saUtr) returns Future.successful(true)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.Authorised
      }
      "return Authorised if multiple authorised userIds are found" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC"))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC"))) returns
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2"))))
          )
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns Future.successful(true)
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          SaAgentReference("654ABC"),
          saUtr) returns Future.successful(true)

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.Authorised
      }
      "return NoRelationship if there are errors when calling CESA" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService.getDelegatedAgentUserIdsFor(
          saUtr) returns
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2")))
        mockMappingConnector.getAgentMappings("sa", arn) returns
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC"))))
        mockEnrolmentStoreProxyAuthorisationService.getAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC"))) returns
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2"))))
          )
        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          saAgentRef,
          saUtr) returns failedResponse

        mockDesAuthorisationService.isAuthorisedInCesa(
          agentCode,
          SaAgentReference("654ABC"),
          saUtr) returns failedResponse

        await(TestService.isAuthorisedForSa(agentCode, saUtr, mtdAuthDetails)) mustBe AccessResponse.NoRelationship
      }
    }
    "no agent reference found for agent" should {
      "return an AccessResponse Error" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)

        val result: AccessResponse = await(
          TestService.isAuthorisedForSa(
            agentCode,
            saUtr,
            AuthDetails(None, None, "ggId", None, None)))

        result mustBe AccessResponse.Error(
          s"No 6 digit agent reference found for agent $agentCode")
      }
    }
  }

  "AuthorisationService.isAuthorisedForPaye" when {
    "not authorised in ESP" should {
      "return NoRelationship" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForPayeInEnrolmentStoreProxy(credId, empRef) returns
          Future.successful(false)

        val result: AccessResponse = await(
          TestService.isAuthorisedForPaye(agentCode, empRef, nonMtdAuthDetails))

        result mustBe AccessResponse.NoRelationship
      }
    }
    "authorised in ESP and not authorised in EBS" should {
      "return NoRelationship" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForPayeInEnrolmentStoreProxy(credId, empRef) returns
          Future.successful(true)
        mockDesAuthorisationService.isAuthorisedInEbs(agentCode, empRef) returns Future
          .successful(false)

        val result: AccessResponse = await(
          TestService.isAuthorisedForPaye(agentCode, empRef, nonMtdAuthDetails))

        result mustBe AccessResponse.NoRelationship
      }
    }
    "authorised in both ESP and EBS" should {
      "return Authorised" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForPayeInEnrolmentStoreProxy(credId, empRef) returns
          Future.successful(true)
        mockDesAuthorisationService.isAuthorisedInEbs(agentCode, empRef) returns Future
          .successful(true)

        val result: AccessResponse = await(
          TestService.isAuthorisedForPaye(agentCode, empRef, nonMtdAuthDetails))

        result mustBe AccessResponse.Authorised
      }
    }
    "an error is thrown by ESP" should {
      "propagate the error" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForPayeInEnrolmentStoreProxy(credId, empRef) returns
          badRequestException

        intercept[BadRequestException] {
          await(
            TestService.isAuthorisedForPaye(agentCode,
                                            empRef,
                                            nonMtdAuthDetails))
        }
      }
    }
    "an error is thrown by EBS" should {
      "propagate the error" in new Setup {
        mockEnrolmentStoreProxyAuthorisationService
          .isAuthorisedForPayeInEnrolmentStoreProxy(credId, empRef) returns
          Future.successful(true)
        mockDesAuthorisationService.isAuthorisedInEbs(agentCode, empRef) returns badRequestException

        intercept[BadRequestException] {
          await(
            TestService.isAuthorisedForPaye(agentCode,
                                            empRef,
                                            nonMtdAuthDetails))
        }
      }
    }
    "agent auth details not provided" should {
      "return an AccessResponse Error" in new Setup {
        val result: AccessResponse = await(
          TestService.isAuthorisedForPaye(
            agentCode,
            empRef,
            AuthDetails(None, None, "", None, None)))

        result mustBe AccessResponse.Error("No user is logged in")
      }
    }
  }

  "AuthorisationService.isAuthorisedForAfi" when {
    "the agent is suspended" should {
      "return AgentSuspended" in new Setup {
        mockAgentClientAuthorisationConnector.getSuspensionDetails(arn) returns
          Future.successful(
            SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

        val result: AccessResponse = await(
          TestService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result mustBe AccessResponse.AgentSuspended
      }
    }
    "the agent is not suspended" should {
      "return NoRelationship if no relationship exists in Agent-FI" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockAgentClientAuthorisationConnector.getSuspensionDetails(arn) returns
          Future.successful(SuspensionDetails.notSuspended)
        mockAfiRelationshipConnector.hasRelationship(arn.value, nino.value) returns Future
          .successful(false)

        val result: AccessResponse = await(
          TestService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result mustBe AccessResponse.NoRelationship
      }
      "return Authorised if a relationship exists in Agent-Fi" in new Setup {
        mockAuditService.sendAuditEvent(
          *[AgentAccessControlEvent],
          *[String],
          *[AgentCode],
          *[String],
          *[TaxIdentifier],
          *[Seq[(String, Any)]]) returns Future.successful(Success)
        mockAgentClientAuthorisationConnector.getSuspensionDetails(arn) returns
          Future.successful(SuspensionDetails.notSuspended)
        mockAfiRelationshipConnector.hasRelationship(arn.value, nino.value) returns Future
          .successful(true)

        val result: AccessResponse = await(
          TestService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result mustBe AccessResponse.Authorised
      }
    }
    "an error is thrown whilst checking for agent suspension" should {
      "return an AccessResponse Error" in new Setup {
        mockAgentClientAuthorisationConnector.getSuspensionDetails(arn) returns failedResponse

        val result: AccessResponse = await(
          TestService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result mustBe AccessResponse
          .Error("Error retrieving suspension details for Arn(arn): boom")
      }
    }
    "an error is thrown whilst checking for an Agent-Fi relationship" should {
      "return an AccessResponse Error" in new Setup {
        mockAgentClientAuthorisationConnector.getSuspensionDetails(arn) returns
          Future.successful(SuspensionDetails.notSuspended)
        mockAfiRelationshipConnector.hasRelationship(arn.value, nino.value) returns failedResponse

        val result: AccessResponse = await(
          TestService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        //TODO the AgentSuspensionChecker is hiding errors thrown in the Agent-FI connector, this needs to be fixed
        result mustBe AccessResponse
          .Error("Error retrieving suspension details for Arn(arn): boom")
      }
    }
    "auth details are not provided" should {
      "return NoRelationship" in new Setup {
        val result: AccessResponse = await(
          TestService
            .isAuthorisedForAfi(agentCode,
                                nino,
                                AuthDetails(None, None, "ggId", None, None)))

        result mustBe AccessResponse.NoRelationship
      }
    }
  }

}
