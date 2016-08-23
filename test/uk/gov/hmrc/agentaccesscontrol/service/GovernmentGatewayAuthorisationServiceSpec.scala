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
import uk.gov.hmrc.agentaccesscontrol.connectors.{AgentDetails, AssignedCredentials, GovernmentGatewayProxyConnector}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class GovernmentGatewayAuthorisationServiceSpec extends UnitSpec with MockitoSugar {

  val ggProxyConnector = mock[GovernmentGatewayProxyConnector]
  val service = new GovernmentGatewayAuthorisationService(ggProxyConnector)
  val utr = new SaUtr("0123456789")
  implicit val hc = new HeaderCarrier()

  "isAuthorisedInGovernmentGateway " should {
    "return true if the agent is assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(AgentDetails("AgentCode", Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(utr, "000111333"))

      result shouldBe true
    }

    "return true if there is more than one agent is assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful
        Seq(AgentDetails("AgentCode", Seq(AssignedCredentials("000111333"), AssignedCredentials("000111444")))))

      val result = await(service.isAuthorisedInGovernmentGateway(utr, "000111444"))

      result shouldBe true
    }

    "return false if the agent is not assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Seq(AgentDetails("AgentCode", Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(utr, "NonMatchingCred"))

      result shouldBe false
    }

    "throw exception if there is more than one agency assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(
        Future successful Seq(
          AgentDetails("AgentCode", Seq(AssignedCredentials("000111333"))),
          AgentDetails("AgentCode1", Seq(AssignedCredentials("000111444")))))

      an[IllegalStateException] should be thrownBy await(service.isAuthorisedInGovernmentGateway(utr, "NonMatchingCred"))
    }

    "throw exception if government gateway proxy fails" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenThrow(new RuntimeException())

      an[RuntimeException] should be thrownBy await(service.isAuthorisedInGovernmentGateway(utr, "NonMatchingCred"))
    }
  }
}
