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

import com.github.tomakehurst.wiremock.client.WireMock
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.CESA_Response
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.model.{DesAgentClientFlagsApiResponse, FoundResponse, NotFoundResponse}
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier


class DesAgentClientApiConnectorISpec extends BaseISpec with MockitoSugar {

  implicit val headerCarrier = HeaderCarrier()
  val agentCode = AgentCode("Agent")

  "getAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in new Context {
      givenClientIsLoggedIn()
        .andIsRelatedToClientInDes(saUtr, "auth_token_33", "env_33").andAuthorisedByBoth648AndI648()

      val connectorWithDifferentHeaders = new DesAgentClientApiConnector(wiremockBaseUrl, "auth_token_33", "env_33", WSHttp, auditService)

      val response: DesAgentClientFlagsApiResponse = await(connectorWithDifferentHeaders.getAgentClientRelationship(saAgentReference, agentCode, saUtr))
      response shouldBe FoundResponse(auth64_8 = true, authI64_8 = true)
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and i64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClientInDes(saUtr).andAuthorisedByBoth648AndI648()

        await(connector.getAgentClientRelationship(saAgentReference, agentCode, saUtr)) shouldBe FoundResponse(auth64_8 = true, authI64_8 = true)
        verify(auditService).auditEvent(Matchers.eq(CESA_Response),
                                        Matchers.eq(agentCode),
                                        Matchers.eq(saUtr),
                                        any[Seq[(String,Any)]])(any[HeaderCarrier])
      }
      "agent is authorised by only i64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClientInDes(saUtr).andIsAuthorisedByOnlyI648()

        await(connector.getAgentClientRelationship(saAgentReference, agentCode, saUtr)) shouldBe FoundResponse(auth64_8 = false, authI64_8 = true)
        verify(auditService).auditEvent(Matchers.eq(CESA_Response),
                                        Matchers.eq(agentCode),
                                        Matchers.eq(saUtr),
                                        any[Seq[(String,Any)]])(any[HeaderCarrier])
      }
      "agent is authorised by only 64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClientInDes(saUtr).andIsAuthorisedByOnly648()

        await(connector.getAgentClientRelationship(saAgentReference, agentCode, saUtr)) shouldBe FoundResponse(auth64_8 = true, authI64_8 = false)
        verify(auditService).auditEvent(Matchers.eq(CESA_Response),
                                        Matchers.eq(agentCode),
                                        Matchers.eq(saUtr),
                                        any[Seq[(String,Any)]])(any[HeaderCarrier])
      }
      "agent is not authorised" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClientInDes(saUtr).butIsNotAuthorised()

        await(connector.getAgentClientRelationship(saAgentReference, agentCode, saUtr)) shouldBe FoundResponse(auth64_8 = false, authI64_8 = false)
        verify(auditService).auditEvent(Matchers.eq(CESA_Response),
                                        Matchers.eq(agentCode),
                                        Matchers.eq(saUtr),
                                        any[Seq[(String,Any)]])(any[HeaderCarrier])
      }
    }

    "return NotFoundResponse in case of a 404" in new Context {
      givenClientIsLoggedIn()
        .andHasNoRelationInDesWith(saUtr)

      await(connector.getAgentClientRelationship(saAgentReference, agentCode, saUtr)) shouldBe NotFoundResponse
    }

    "fail in any other cases, like internal server error" in new Context {
      givenClientIsLoggedIn().andDesIsDown()

      an[Exception] should be thrownBy await(connector.getAgentClientRelationship(saAgentReference, agentCode, saUtr))
    }
  }

  private abstract class Context {
    val auditService = mock[AuditService]
    val connector = new DesAgentClientApiConnector(wiremockBaseUrl, "secret", "test", WSHttp, auditService)
    val saAgentReference = SaAgentReference("AGENTR")
    val saUtr = SaUtr("SAUTR456")

    def givenClientIsLoggedIn() =
      given()
        .agentAdmin("ABCDEF122345").isLoggedIn()
        .andHasSaAgentReferenceWithEnrolment(saAgentReference)
  }
}
