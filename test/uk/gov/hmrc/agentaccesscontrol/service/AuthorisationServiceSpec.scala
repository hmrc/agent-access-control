/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.service

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.connectors.{AuthConnector, AuthDetails}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class AuthorisationServiceSpec extends UnitSpec with MockitoSugar {
  val agentCode = AgentCode("ABCDEF123456")
  val saAgentRef = SaAgentReference("ABC456")
  val clientSaUtr = SaUtr("CLIENTSAUTR456")


  implicit val headerCarrier = HeaderCarrier()


  "isAuthorised" should {
    "return false if SA agent reference cannot be found (as CESA cannot be checked)" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(None, "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe false
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> false, "affinityGroup" -> "Agent", "agentUserRole" -> "admin"))
    }

    "return false if SA agent reference is found and CesaAuthorisationService returns false and GG Authorisation returns true" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(Some(saAgentRef), "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))
      when(mockGGAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, "ggId", clientSaUtr)).thenReturn(true)
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(false)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe false
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> false, "cesa" -> false, "ggw" -> true, "saAgentReference" -> saAgentRef, "affinityGroup" -> "Agent", "agentUserRole" -> "admin"))
    }

    "return true if SA agent reference is found and CesaAuthorisationService returns true and GG Authorisation returns true" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(Some(saAgentRef), "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))
      when(mockGGAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, "ggId", clientSaUtr)).thenReturn(true)
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(true)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe true
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> true, "cesa" -> true, "ggw" -> true, "saAgentReference" -> saAgentRef, "affinityGroup" -> "Agent", "agentUserRole" -> "admin"))
    }

    "not hard code audited values" in new Context {
      val differentSaAgentRef = SaAgentReference("XYZ123")

      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(Some(differentSaAgentRef), "ggId", affinityGroup = Some("Organisation"), agentUserRole = Some("assistant"))))
      when(mockGGAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, "ggId", clientSaUtr)).thenReturn(true)
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, differentSaAgentRef, clientSaUtr))
        .thenReturn(true)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe true
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> true, "cesa" -> true, "ggw" -> true, "saAgentReference" -> differentSaAgentRef, "affinityGroup" -> "Organisation", "agentUserRole" -> "assistant"))
    }

    "still work if the fields only used for auditing are removed from the auth record" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(Some(saAgentRef), "ggId", affinityGroup = None, agentUserRole = None)))
      when(mockGGAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, "ggId", clientSaUtr)).thenReturn(true)
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(true)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe true
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> true, "cesa" -> true, "ggw" -> true, "saAgentReference" -> saAgentRef))
    }

    "return false if SA agent reference is found and CesaAuthorisationService returns true and GG Authorisation returns false" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(Some(saAgentRef), "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))
      when(mockGGAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, "ggId", clientSaUtr)).thenReturn(false)
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(true)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe false
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> false, "cesa" -> true, "ggw" -> false, "saAgentReference" -> saAgentRef, "affinityGroup" -> "Agent", "agentUserRole" -> "admin"))
    }

    "return false if SA agent reference is found and CesaAuthorisationService returns false and GG Authorisation returns false" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Some(AuthDetails(Some(saAgentRef), "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))
      when(mockGGAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, "ggId", clientSaUtr)).thenReturn(false)
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(false)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe false
      verify(mockAuditService).auditEvent(AgentAccessControlDecision, "agent access decision", "/request-path", agentCode, clientSaUtr,
        Seq("credId" -> "ggId", "accessGranted" -> false, "cesa" -> false, "ggw" -> false, "saAgentReference" -> saAgentRef, "affinityGroup" -> "Agent", "agentUserRole" -> "admin"))
    }

    "return false if user is not logged in" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(None)
      await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path")) shouldBe false
    }

    "propagate any errors that happened" in new Context {
      when(mockAuthConnector.currentAuthDetails()).thenReturn(Future failed new BadRequestException("bad request"))

      intercept[BadRequestException] {
        await(authorisationService.isAuthorised(agentCode, clientSaUtr, "/request-path"))
      }
    }
  }

  private abstract class Context {
    val mockAuthConnector = mock[AuthConnector]
    val mockCesaAuthorisationService = mock[CesaAuthorisationService]
    val mockGGAuthorisationService = mock[GovernmentGatewayAuthorisationService]
    val mockAuditService = mock[AuditService]
    val authorisationService = new AuthorisationService(
      mockCesaAuthorisationService,
      mockAuthConnector,
      mockGGAuthorisationService,
      mockAuditService)
  }
}
