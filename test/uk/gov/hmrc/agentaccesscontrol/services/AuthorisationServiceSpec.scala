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
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitTest
import uk.gov.hmrc.agentaccesscontrol.mocks.connectors.{
  MockACAConnector,
  MockAfiRelationshipConnector,
  MockMappingConnector
}
import uk.gov.hmrc.agentaccesscontrol.mocks.services.{
  MockAuditService,
  MockDesAuthorisationService,
  MockESPAuthorisationService
}
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

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationServiceSpec
    extends UnitTest
    with MockDesAuthorisationService
    with MockESPAuthorisationService
    with MockAuditService
    with MockMappingConnector
    with MockAfiRelationshipConnector
    with MockACAConnector {

  val authorisationService = new AuthorisationService(
    mockDesAuthorisationService,
    mockESPAuthorisationService,
    mockAuditService,
    mockMappingConnector,
    mockAfiRelationshipConnector,
    mockACAConnector
  )

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
      "return NoRelationship if user is not authorised for SA in ESP" in {
        mockSendAuditEvent
        mockIsAuthorisedForSaInEnrolmentStoreProxy(credId,
                                                   saUtr,
                                                   Future.successful(false))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            nonMtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return NoRelationship if user is authorised for SA in ESP, but not authorised for SA in CESA" in {
        mockSendAuditEvent
        mockIsAuthorisedForSaInEnrolmentStoreProxy(credId,
                                                   saUtr,
                                                   Future.successful(true))
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               Future.successful(false))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            nonMtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return Authorised if user is authorised in both ESP and CESA" in {
        mockSendAuditEvent
        mockIsAuthorisedForSaInEnrolmentStoreProxy(credId,
                                                   saUtr,
                                                   Future.successful(true))
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               Future.successful(true))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            nonMtdAuthDetails)) shouldBe AccessResponse.Authorised
      }
      "return an Error if ESP throws an error" in {
        mockIsAuthorisedForSaInEnrolmentStoreProxy(credId,
                                                   saUtr,
                                                   badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForSa(agentCode,
                                                   saUtr,
                                                   nonMtdAuthDetails))
        }
      }
      "return an Error if CESA throws an error" in {
        mockIsAuthorisedForSaInEnrolmentStoreProxy(credId,
                                                   saUtr,
                                                   Future.successful(true))
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForSa(agentCode,
                                                   saUtr,
                                                   nonMtdAuthDetails))
        }
      }
    }
    "mtdAgentAuthDetails provided" should {
      "return NoRelationship if no DelegatedAgentUserIds found in ESP" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(saUtr, Future.successful(Set.empty))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return an exception if there is an error when calling ESP to GetDelegatedAgentUserIds" in {
        mockGetDelegatedAgentUserIdsFor(saUtr, badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForSa(agentCode,
                                                   saUtr,
                                                   mtdAuthDetails))
        }
      }
      "return NoRelationship if no agent mappings are found" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(AgentReferenceMappings(List.empty)))
        //TODO no need to make this call if we got an empty list previously
        mockGetAgentUserIdsFor(List.empty, Future.successful(Seq.empty))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return NoRelationship if no agent user ids are found in Agent Mappings or ESP" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value)))))
        mockGetAgentUserIdsFor(List(saAgentRef), Future.successful(Seq.empty))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return an exception if there is an error when calling Agent Mappings" in {
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings("sa", arn, badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForSa(agentCode,
                                                   saUtr,
                                                   mtdAuthDetails))
        }
      }
      "return an exception if there is an error when calling ESP GetAgentUserIds" in {
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value)))))
        mockGetAgentUserIdsFor(List(saAgentRef), badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForSa(agentCode,
                                                   saUtr,
                                                   mtdAuthDetails))
        }
      }
      "return NoRelationship if userIds found, but do not match" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value)))))
        mockGetAgentUserIdsFor(
          List(saAgentRef),
          Future.successful(Seq((saAgentRef, Set(AgentUserId("cred2"))))))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return NoRelationship if userIds found, but not authorised in CESA" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value)))))
        mockGetAgentUserIdsFor(
          List(saAgentRef),
          Future.successful(Seq((saAgentRef, Set(AgentUserId("cred1"))))))
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               Future.successful(false))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
      "return Authorised if there is one agent user id and it is authorised in CESA" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value)))))
        mockGetAgentUserIdsFor(
          List(saAgentRef),
          Future.successful(Seq((saAgentRef, Set(AgentUserId("cred1"))))))
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               Future.successful(true))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.Authorised
      }
      "return Authorised if the first agent user id is not authorised, but the second is" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC")))))
        mockGetAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC")),
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2")))))
        )
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               Future.successful(false))
        mockIsAuthorisedInCesa(agentCode,
                               SaAgentReference("654ABC"),
                               saUtr,
                               Future.successful(true))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.Authorised
      }
      "return Authorised if the first agent user id gets an error from CESA, but the second gets an authorised response" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC")))))
        mockGetAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC")),
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2")))))
        )
        mockIsAuthorisedInCesa(agentCode, saAgentRef, saUtr, failedResponse)
        mockIsAuthorisedInCesa(agentCode,
                               SaAgentReference("654ABC"),
                               saUtr,
                               Future.successful(true))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.Authorised
      }
      "return Authorised if multiple authorised userIds are found" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC")))))
        mockGetAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC")),
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2")))))
        )
        mockIsAuthorisedInCesa(agentCode,
                               saAgentRef,
                               saUtr,
                               Future.successful(true))
        mockIsAuthorisedInCesa(agentCode,
                               SaAgentReference("654ABC"),
                               saUtr,
                               Future.successful(true))

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.Authorised
      }
      "return NoRelationship if there are errors when calling CESA" in {
        mockSendAuditEvent
        mockGetDelegatedAgentUserIdsFor(
          saUtr,
          Future.successful(Set(AgentUserId("cred1"), AgentUserId("cred2"))))
        mockGetAgentMappings(
          "sa",
          arn,
          Future.successful(
            AgentReferenceMappings(
              List(AgentReferenceMapping(arn.value, saAgentRef.value),
                   AgentReferenceMapping("nra", "654ABC")))))
        mockGetAgentUserIdsFor(
          List(saAgentRef, SaAgentReference("654ABC")),
          Future.successful(
            Seq((saAgentRef, Set(AgentUserId("cred1"))),
                (SaAgentReference("654ABC"), Set(AgentUserId("cred2")))))
        )
        mockIsAuthorisedInCesa(agentCode, saAgentRef, saUtr, failedResponse)
        mockIsAuthorisedInCesa(agentCode,
                               SaAgentReference("654ABC"),
                               saUtr,
                               failedResponse)

        await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            mtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      }
    }
    "no agent reference found for agent" should {
      "return an AccessResponse Error" in {
        mockSendAuditEvent

        val result = await(
          authorisationService.isAuthorisedForSa(
            agentCode,
            saUtr,
            AuthDetails(None, None, "ggId", None, None)))

        result shouldBe AccessResponse.Error(
          s"No 6 digit agent reference found for agent $agentCode")
      }
    }
  }

  "AuthorisationService.isAuthorisedForPaye" when {
    "not authorised in ESP" should {
      "return NoRelationship" in {
        mockSendAuditEvent
        mockIsAuthorisedForPayeInEnrolmentStoreProxy(credId,
                                                     empRef,
                                                     Future.successful(false))

        val result = await(
          authorisationService.isAuthorisedForPaye(agentCode,
                                                   empRef,
                                                   nonMtdAuthDetails))

        result shouldBe AccessResponse.NoRelationship
      }
    }
    "authorised in ESP and not authorised in EBS" should {
      "return NoRelationship" in {
        mockSendAuditEvent
        mockIsAuthorisedForPayeInEnrolmentStoreProxy(credId,
                                                     empRef,
                                                     Future.successful(true))
        mockIsAuthorisedInEbs(agentCode, empRef, Future.successful(false))

        val result = await(
          authorisationService.isAuthorisedForPaye(agentCode,
                                                   empRef,
                                                   nonMtdAuthDetails))

        result shouldBe AccessResponse.NoRelationship
      }
    }
    "authorised in both ESP and EBS" should {
      "return Authorised" in {
        mockSendAuditEvent
        mockIsAuthorisedForPayeInEnrolmentStoreProxy(credId,
                                                     empRef,
                                                     Future.successful(true))
        mockIsAuthorisedInEbs(agentCode, empRef, Future.successful(true))

        val result = await(
          authorisationService.isAuthorisedForPaye(agentCode,
                                                   empRef,
                                                   nonMtdAuthDetails))

        result shouldBe AccessResponse.Authorised
      }
    }
    "an error is thrown by ESP" should {
      "propagate the error" in {
        mockIsAuthorisedForPayeInEnrolmentStoreProxy(credId,
                                                     empRef,
                                                     badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForPaye(agentCode,
                                                     empRef,
                                                     nonMtdAuthDetails))
        }
      }
    }
    "an error is thrown by EBS" should {
      "propagate the error" in {
        mockIsAuthorisedForPayeInEnrolmentStoreProxy(credId,
                                                     empRef,
                                                     Future.successful(true))
        mockIsAuthorisedInEbs(agentCode, empRef, badRequestException)

        intercept[BadRequestException] {
          await(
            authorisationService.isAuthorisedForPaye(agentCode,
                                                     empRef,
                                                     nonMtdAuthDetails))
        }
      }
    }
    "agent auth details not provided" should {
      "return an AccessResponse Error" in {
        val result = await(
          authorisationService.isAuthorisedForPaye(
            agentCode,
            empRef,
            AuthDetails(None, None, "", None, None)))

        result shouldBe AccessResponse.Error("No user is logged in")
      }
    }
  }

  "AuthorisationService.isAuthorisedForAfi" when {
    "the agent is suspended" should {
      "return AgentSuspended" in {
        mockGetSuspensionDetails(
          arn,
          Future.successful(
            SuspensionDetails(suspensionStatus = true, Some(Set("AGSV")))))

        val result = await(
          authorisationService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result shouldBe AccessResponse.AgentSuspended
      }
    }
    "the agent is not suspended" should {
      "return NoRelationship if no relationship exists in Agent-FI" in {
        mockSendAuditEvent
        mockGetSuspensionDetails(
          arn,
          Future.successful(SuspensionDetails.notSuspended))
        mockHasRelationship(arn.value, nino.value, Future.successful(false))

        val result = await(
          authorisationService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result shouldBe AccessResponse.NoRelationship
      }
      "return Authorised if a relationship exists in Agent-Fi" in {
        mockSendAuditEvent
        mockGetSuspensionDetails(
          arn,
          Future.successful(SuspensionDetails.notSuspended))
        mockHasRelationship(arn.value, nino.value, Future.successful(true))

        val result = await(
          authorisationService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result shouldBe AccessResponse.Authorised
      }
    }
    "an error is thrown whilst checking for agent suspension" should {
      "return an AccessResponse Error" in {
        mockGetSuspensionDetails(arn, failedResponse)

        val result = await(
          authorisationService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        result shouldBe AccessResponse
          .Error("Error retrieving suspension details for Arn(arn): boom")
      }
    }
    "an error is thrown whilst checking for an Agent-Fi relationship" should {
      "return an AccessResponse Error" in {
        mockGetSuspensionDetails(
          arn,
          Future.successful(SuspensionDetails.notSuspended))
        mockHasRelationship(arn.value, nino.value, failedResponse)

        val result = await(
          authorisationService
            .isAuthorisedForAfi(agentCode, nino, afiAuthDetails))

        //TODO the AgentSuspensionChecker is hiding errors thrown in the Agent-FI connector, this needs to be fixed
        result shouldBe AccessResponse
          .Error("Error retrieving suspension details for Arn(arn): boom")
      }
    }
    "auth details are not provided" should {
      "return NoRelationship" in {
        val result = await(
          authorisationService
            .isAuthorisedForAfi(agentCode,
                                nino,
                                AuthDetails(None, None, "ggId", None, None)))

        result shouldBe AccessResponse.NoRelationship
      }
    }
  }

}
