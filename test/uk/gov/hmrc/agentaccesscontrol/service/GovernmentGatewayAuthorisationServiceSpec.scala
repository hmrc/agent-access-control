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

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.connectors.{AssignedAgent, AssignedCredentials, GovernmentGatewayProxyConnector}
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class GovernmentGatewayAuthorisationServiceSpec extends UnitSpec with MockitoSugar {

  val ggProxyConnector = mock[GovernmentGatewayProxyConnector]
  val service = new GovernmentGatewayAuthorisationService(ggProxyConnector)
  val agentCode = AgentCode("12AAAAA3A456")
  val agentCode2 = AgentCode("23BBBBB4B567")
  val utr = SaUtr("0123456789")
  implicit val hc = new HeaderCarrier()

  "isAuthorisedInGovernmentGateway " should {
    "return true if the client is allocated to the agency and assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "000111333"))

      result shouldBe true
    }

    "return true if there is more than one agent is assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful
        Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111333"), AssignedCredentials("000111444")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "000111444"))

      result shouldBe true
    }

    "return false if the client is allocated to the agency but not assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111444")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "000111333"))

      result shouldBe false
    }

    // we don't expect the GG to allow things to be set up like this, so this test is just here to be on the safe side
    "return false if the client is not allocated to the agency but is assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(AssignedAgent(agentCode2, Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "000111333"))

      result shouldBe false
    }

    // we don't expect the GG to allow things to be set up like this, so this test is just here to be on the safe side
    "return false if client is allocated to the agency and is assigned to the agent credential inside a different agency" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(
        AssignedAgent(agentCode, Seq(AssignedCredentials("000111444"))),
        AssignedAgent(agentCode2, Seq(AssignedCredentials("000111333")))
      ))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "000111333"))

      result shouldBe false

    }

    "return false if the client is neither allocated to the agency nor assigned to the agent credential" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(AssignedAgent(agentCode, Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "NonMatchingCred"))

      result shouldBe false
    }

    "return true there is more than one agency assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(
        Future successful Seq(
          AssignedAgent(agentCode, Seq(AssignedCredentials("000111333"))),
          AssignedAgent(agentCode2, Seq(AssignedCredentials("000111444")))))

      await(service.isAuthorisedInGovernmentGateway(agentCode2, utr, "000111444")) shouldBe true
    }

    "throw exception if government gateway proxy fails" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenThrow(new RuntimeException())

      an[RuntimeException] should be thrownBy await(service.isAuthorisedInGovernmentGateway(agentCode, utr, "NonMatchingCred"))
    }
  }
}
