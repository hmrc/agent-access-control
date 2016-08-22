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

import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.model.{FoundResponse, NotFoundResponse}
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.{SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier


class DesAgentClientApiConnectorISpec extends BaseISpec {

  implicit val headerCarrier = HeaderCarrier()

  "getAgentClientRelationship" should {
    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and i64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClient(saUtr).andAuthorisedByBoth648AndI648()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = true, authI64_8 = true)
      }
      "agent is authorised by only i64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClient(saUtr).andIsAuthorisedByOnlyI648()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = false, authI64_8 = true)
      }
      "agent is authorised by only 64-8" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClient(saUtr).andIsAuthorisedByOnly648()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = true, authI64_8 = false)
      }
      "agent is not authorised" in new Context {
        givenClientIsLoggedIn()
          .andIsRelatedToClient(saUtr).butIsNotAuthorised()

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = false, authI64_8 = false)
      }
    }

    "return NotFoundResponse in case of a 404" in new Context {
      givenClientIsLoggedIn()
        .andHasNoRelationInDesWith(saUtr)

      await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe NotFoundResponse
    }

    "fail in any other cases, like internal server error" in new Context {
      givenClientIsLoggedIn().andDesIsDown()

      an[Exception] should be thrownBy await(connector.getAgentClientRelationship(saAgentReference, saUtr))
    }
  }

  private abstract class Context {
    val connector = new DesAgentClientApiConnector(wiremockBaseUrl, WSHttp)
    val saAgentReference = SaAgentReference("AGENTR")
    val saUtr = SaUtr("SAUTR456")

    def givenClientIsLoggedIn() =
      given()
        .agentAdmin("ABCDEF122345").isLoggedIn()
        .andHasSaAgentReferenceWithEnrolment(saAgentReference)
  }
}
