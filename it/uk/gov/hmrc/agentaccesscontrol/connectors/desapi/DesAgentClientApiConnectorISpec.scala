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

package uk.gov.hmrc.agentaccesscontrol.connectors.desapi

import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportAppPerSuite, WireMockWithOneAppPerSuiteISpec}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class DesAgentClientApiConnectorISpec
    extends WireMockWithOneAppPerSuiteISpec
    with MockitoSugar
    with MetricTestSupportAppPerSuite {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val desApiConnector: DesAgentClientApiConnector = app.injector.instanceOf[DesAgentClientApiConnector]

  val saAgentReference = SaAgentReference("AGENTR")
  val saUtr = SaUtr("SAUTR456")
  val empRef = EmpRef("123", "4567890")
  val agentCode = AgentCode("A1234567890A")
  val providerId = "12345-credId"

  def givenClientIsLoggedIn() =
    given()
      .agentAdmin(agentCode, providerId, Some(saAgentReference), None)
      .isAuthenticated()

  "getSaAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in {
      givenClientIsLoggedIn()
        .andIsRelatedToSaClientInDes(saUtr)
        .andAuthorisedByBoth648AndI648()
      givenCleanMetricRegistry()

      val response = await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr))
      response shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetSaAgentClientRelationship-GET")
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and i64-8" in {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr)
          .andAuthorisedByBoth648AndI648()

        await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
          auth64_8 = true,
          authI64_8 = true)
      }
      "agent is authorised by only i64-8" in {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr)
          .andIsAuthorisedByOnlyI648()

        await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
          auth64_8 = false,
          authI64_8 = true)
      }
      "agent is authorised by only 64-8" in {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr)
          .andIsAuthorisedByOnly648()

        await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
          auth64_8 = true,
          authI64_8 = false)
      }
      "agent is not authorised" in {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr)
          .butIsNotAuthorised()

        await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
          auth64_8 = false,
          authI64_8 = false)
      }
    }

    "return NotFoundResponse in case of a 404" in {
      givenClientIsLoggedIn()
        .andHasNoRelationInDesWith(saUtr)

      await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaNotFoundResponse
    }

    "fail in any other cases, like internal server error" in {
      givenClientIsLoggedIn().andDesIsDown()

      an[Exception] should be thrownBy await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr))
    }

    "log metrics for the outbound call" in {
      givenClientIsLoggedIn()
        .andIsRelatedToSaClientInDes(saUtr)
        .andAuthorisedByBoth648AndI648()
      givenCleanMetricRegistry()

      await(desApiConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
        auth64_8 = true,
        authI64_8 = true)
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetSaAgentClientRelationship-GET")
    }

    "audit outbound DES call" in {
      givenClientIsLoggedIn()
        .andIsRelatedToSaClientInDes(saUtr)
        .andAuthorisedByBoth648AndI648()
    }
  }

  "getPayeAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in {
      givenClientIsLoggedIn()
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()
      givenCleanMetricRegistry()

      val response = await(desApiConnector.getPayeAgentClientRelationship(agentCode, empRef))
      response shouldBe PayeFoundResponse(auth64_8 = true)
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetPayeAgentClientRelationship-GET")
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8" in {
        givenClientIsLoggedIn()
          .andIsRelatedToPayeClientInDes(empRef)
          .andIsAuthorisedBy648()

        await(desApiConnector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true)
      }
      "agent is not authorised" in {
        givenClientIsLoggedIn()
          .andIsRelatedToPayeClientInDes(empRef)
          .butIsNotAuthorised()

        await(desApiConnector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = false)
      }
    }

    "return NotFoundResponse in case of a 404" in {
      givenClientIsLoggedIn()
        .andHasNoRelationInDesWith(empRef)

      await(desApiConnector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeNotFoundResponse
    }

    "fail in any other cases, like internal server error" in {
      givenClientIsLoggedIn().andDesIsDown()

      an[Exception] should be thrownBy await(desApiConnector.getPayeAgentClientRelationship(agentCode, empRef))
    }

    "log metrics for outbound call" in {
      givenClientIsLoggedIn()
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()
      givenCleanMetricRegistry()

      await(desApiConnector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true)
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetPayeAgentClientRelationship-GET")
    }

    "audit outbound call to DES" in {
      givenClientIsLoggedIn()
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()
    }
  }
}
