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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.agentaccesscontrol.StartAndStopWireMock
import uk.gov.hmrc.agentaccesscontrol.model.{FoundResponse, NotFoundResponse}
import uk.gov.hmrc.agentaccesscontrol.support.DesStubHelper
import uk.gov.hmrc.domain.{SaAgentReference, AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec


class DesAgentClientApiConnectorISpec extends UnitSpec
  with OneServerPerSuite
  with StartAndStopWireMock
  with DesStubHelper {

  implicit val headerCarrier = HeaderCarrier()

  "getAgentClientRelationship" should {
    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and i64-8" in new Context {
        desAgentClientFlagsRequest(saAgentReference, saUtr)
          .willReturn(aResponse().withStatus(200).withBody(
            """
              |{
              |    "Auth_64-8": true,
              |    "Auth_i64-8": true
              |}
            """.stripMargin))

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = true, authI64_8 = true)
      }
      "agent is authorised by only i64-8" in new Context {
        desAgentClientFlagsRequest(saAgentReference, saUtr).willReturnFlags(false, true)

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = false, authI64_8 = true)
      }
      "agent is authorised by only 64-8" in new Context {
        desAgentClientFlagsRequest(saAgentReference, saUtr).willReturnFlags(true, false)

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = true, authI64_8 = false)
      }
      "agent is not authorised" in new Context {
        desAgentClientFlagsRequest(saAgentReference, saUtr).willReturnFlags(false, false)

        await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe FoundResponse(auth64_8 = false, authI64_8 = false)
      }
    }

    "return NotFoundResponse in case of a 404" in new Context {
      desAgentClientFlagsRequest(saAgentReference, saUtr).willReturnStatus(404)

      await(connector.getAgentClientRelationship(saAgentReference, saUtr)) shouldBe NotFoundResponse
    }

    "fail in any other cases, like internal server error" in new Context {
      desAgentClientFlagsRequest(saAgentReference, saUtr).willReturnStatus(500)

      an[Exception] should be thrownBy await(connector.getAgentClientRelationship(saAgentReference, saUtr))
    }
  }

  private abstract class Context {
    val connector = new DesAgentClientApiConnector(wiremockBaseUrl)
    val saAgentReference = SaAgentReference("AGENTR")
    val saUtr = SaUtr("SAUTR456")
  }
}
