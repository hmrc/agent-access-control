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

import com.kenshoo.play.metrics.MetricsRegistry
import org.mockito.Matchers.{any, eq => eqs}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.audit.model.MergedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext


class DesAgentClientApiConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MockitoSugar {

  implicit val headerCarrier = HeaderCarrier()

  "getAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in new Context {
      givenClientIsLoggedIn()
        .andIsRelatedToSaClientInDes(saUtr, "auth_token_33", "env_33").andAuthorisedByBoth648AndI648()

      val connectorWithDifferentHeaders = new DesAgentClientApiConnector(wiremockBaseUrl, "auth_token_33", "env_33", wsHttp)

      val response = await(connectorWithDifferentHeaders.getAgentClientRelationship(saAgentReference, saUtr))
      response shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and i64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr).andAuthorisedByBoth648AndI648()

        when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext])).thenThrow(new RuntimeException("EXCEPTION!"))

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
      }
      "agent is authorised by only i64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr).andIsAuthorisedByOnlyI648()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(auth64_8 = false, authI64_8 = true)
      }
      "agent is authorised by only 64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr).andIsAuthorisedByOnly648()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = false)
      }
      "agent is not authorised" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToSaClientInDes(saUtr).butIsNotAuthorised()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(auth64_8 = false, authI64_8 = false)
      }
    }

    "return NotFoundResponse in case of a 404" in new Context {
      givenClientIsLoggedIn()
        .andHasNoRelationInDesWith(saUtr)

      await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaNotFoundResponse
    }

    "fail in any other cases, like internal server error" in new Context {
      givenClientIsLoggedIn().andDesIsDown()

      an[Exception] should be thrownBy await(connector.getAgentClientRelationship(saAgentReference, saUtr))
    }

    "log metrics for the outbound call" in new Context {
      val metricsRegistry = MetricsRegistry.defaultRegistry
      givenClientIsLoggedIn()
        .andIsRelatedToSaClientInDes(saUtr).andAuthorisedByBoth648AndI648()

      await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
      metricsRegistry.getTimers.get("Timer-ConsumedAPI-DES-GetSaAgentClientRelationship-GET").getCount should be >= 1L
    }

    "audit oubound DES call" in new Context {
      givenClientIsLoggedIn()
        .andIsRelatedToSaClientInDes(saUtr).andAuthorisedByBoth648AndI648()

      when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext])).thenThrow(new RuntimeException("EXCEPTION!"))

      await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
      outboundSaCallToDesShouldBeAudited(auth64_8 = true, authI64_8 = true)
    }
  }

  "getPayeAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in new Context {
      givenClientIsLoggedIn()
        .andIsRelatedToPayeClientInDes(empRef, "auth_token_33", "env_33").andAuthorisedByBoth648AndOAA()

      val connectorWithDifferentHeaders = new DesAgentClientApiConnector(wiremockBaseUrl, "auth_token_33", "env_33", wsHttp)

      val response = await(connectorWithDifferentHeaders.getPayeAgentClientRelationship(agentCode, empRef))
      response shouldBe PayeFoundResponse(auth64_8 = true, authOAA = true)
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and OAA" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToPayeClientInDes(empRef).andAuthorisedByBoth648AndOAA()

        when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext])).thenThrow(new RuntimeException("EXCEPTION!"))

        await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true, authOAA = true)
      }
      "agent is authorised by only OAA" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToPayeClientInDes(empRef).andIsAuthorisedByOnlyOAA()

        await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = false, authOAA = true)
      }
      "agent is authorised by only 64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToPayeClientInDes(empRef).andIsAuthorisedByOnly648()

        await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true, authOAA = false)
      }
      "agent is not authorised" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToPayeClientInDes(empRef).butIsNotAuthorised()

        await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = false, authOAA = false)
      }
    }

    "return NotFoundResponse in case of a 404" in new Context {
      givenClientIsLoggedIn()
        .andHasNoRelationInDesWith(empRef)

      await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeNotFoundResponse
    }

    "fail in any other cases, like internal server error" in new Context {
      givenClientIsLoggedIn().andDesIsDown()

      an[Exception] should be thrownBy await(connector.getPayeAgentClientRelationship(agentCode, empRef))
    }

    "log metrics for outbound call" in new Context {
      val metricsRegistry = MetricsRegistry.defaultRegistry
      givenClientIsLoggedIn()
        .andIsRelatedToPayeClientInDes(empRef).andAuthorisedByBoth648AndOAA()

      await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true, authOAA = true)
      metricsRegistry.getTimers.get("Timer-ConsumedAPI-DES-GetPayeAgentClientRelationship-GET").getCount should be >= 1L
    }
    "audit outbound call to DES" in new Context {
      givenClientIsLoggedIn()
        .andIsRelatedToPayeClientInDes(empRef).andAuthorisedByBoth648AndOAA()

      when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext])).thenThrow(new RuntimeException("EXCEPTION!"))

      await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true, authOAA = true)
      outboundPayeCallToDesShouldBeAudited(auth64_8 = true, authOAA = true)
    }
  }

  private abstract class Context extends MockAuditingContext {

    val connector = new DesAgentClientApiConnector(wiremockBaseUrl, "secret", "test", wsHttp)
    val saAgentReference = SaAgentReference("AGENTR")
    val saUtr = SaUtr("SAUTR456")
    val empRef = EmpRef("123", "4567890")
    val agentCode = AgentCode("A1234567890A")

    def givenClientIsLoggedIn() =
      given()
        .agentAdmin(agentCode.value).isLoggedIn()
        .andHasSaAgentReferenceWithEnrolment(saAgentReference)

    def outboundSaCallToDesShouldBeAudited(auth64_8: Boolean, authI64_8: Boolean): Unit = {
      val event: MergedDataEvent = capturedEvent()

      event.auditType shouldBe "OutboundCall"

      event.request.tags("path") shouldBe s"$wiremockBaseUrl/sa/agents/$saAgentReference/client/$saUtr"

      val responseJson = Json.parse(event.response.detail("responseMessage"))
      (responseJson \ "Auth_64-8").as[Boolean] shouldBe auth64_8
      (responseJson \ "Auth_i64-8").as[Boolean] shouldBe authI64_8
    }

    def outboundPayeCallToDesShouldBeAudited(auth64_8: Boolean, authOAA: Boolean): Unit = {
      val event: MergedDataEvent = capturedEvent()

      event.auditType shouldBe "OutboundCall"

      event.request.tags("path") shouldBe s"$wiremockBaseUrl/agents/regime/PAYE/agentref/$agentCode/clientref/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}"

      val responseJson = Json.parse(event.response.detail("responseMessage"))
      (responseJson \ "Auth_64-8").as[Boolean] shouldBe auth64_8
      (responseJson \ "Auth_OAA").as[Boolean] shouldBe authOAA
    }
  }
}
