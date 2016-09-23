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

import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors.{AssignedAgent, AssignedCredentials, GovernmentGatewayProxyConnector}
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class GovernmentGatewayAuthorisationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val ggProxyConnector = mock[GovernmentGatewayProxyConnector]
  val auditService = mock[AuditService]
  val service = new GovernmentGatewayAuthorisationService(ggProxyConnector, auditService)
  val agentCode = AgentCode("12AAAAA3A456")
  val agentCode2 = AgentCode("23BBBBB4B567")
  val utr = SaUtr("0123456789")
  implicit val hc = new HeaderCarrier()


  "isAuthorisedInGovernmentGateway " should {
    "return true if the client is allocated to the agency and assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc))
        .thenReturn(Future successful Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, "000111333", utr))

      result shouldBe true
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode, utr, Seq("ggCredentialId" -> "000111333", "result" -> true))
    }

    "return true if there is more than one agent assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc)).thenReturn(Future successful
        Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111333"), AssignedCredentials("000111444")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, "000111444", utr))

      result shouldBe true
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode, utr, Seq("ggCredentialId" -> "000111444", "result" -> true))
    }

    "return false if the client is allocated to the agency but not assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc))
        .thenReturn(Future successful Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111444")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, "000111333", utr))

      result shouldBe false
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode, utr, Seq("ggCredentialId" -> "000111333", "result" -> false))
    }

    // we don't expect the GG to allow things to be set up like this, so this test is just here to be on the safe side
    "return false if the client is not allocated to the agency but is assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc))
        .thenReturn(Future successful Seq(AssignedAgent(agentCode2, Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, "000111333", utr))

      result shouldBe false
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode, utr, Seq("ggCredentialId" -> "000111333", "result" -> false))
    }

    // we don't expect the GG to allow things to be set up like this, so this test is just here to be on the safe side
    "return false if client is allocated to the agency and is assigned to the agent credential inside a different agency" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc)).thenReturn(Future successful Seq(
        AssignedAgent(agentCode, Seq(AssignedCredentials("000111444"))),
        AssignedAgent(agentCode2, Seq(AssignedCredentials("000111333")))
      ))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, "000111333", utr))

      result shouldBe false
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode, utr, Seq("ggCredentialId" -> "000111333", "result" -> false))
    }

    "return false if the client is neither allocated to the agency nor assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc))
            .thenReturn(Future successful Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, "NonMatchingCred", utr))

      result shouldBe false
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode, utr, Seq("ggCredentialId" -> "NonMatchingCred", "result" -> false))
    }

    "return true if the client is allocated to more than one agency, and one of them is matching" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode2)(hc)).thenReturn(
        Future successful Seq(
          AssignedAgent(agentCode, Seq(AssignedCredentials("000111333"))),
          AssignedAgent(agentCode2, Seq(AssignedCredentials("000111444")))))

      await(service.isAuthorisedInGovernmentGateway(agentCode2, "000111444", utr)) shouldBe true
      verify(auditService).auditEvent(AgentAccessControlEvent.GGW_Decision, agentCode2, utr, Seq("ggCredentialId" -> "000111444", "result" -> true))
    }

    "throw exception if government gateway proxy fails" in {
      when(ggProxyConnector.getAssignedSaAgents(utr, agentCode)(hc)).thenThrow(new RuntimeException())

      an[RuntimeException] should be thrownBy await(service.isAuthorisedInGovernmentGateway(agentCode, "NonMatchingCred", utr))
    }
  }

  override protected def beforeEach(): Unit = {
    Mockito.reset(auditService, ggProxyConnector)
  }
}
