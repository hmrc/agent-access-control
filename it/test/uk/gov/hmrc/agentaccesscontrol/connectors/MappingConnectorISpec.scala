/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.models.AgentReferenceMapping
import uk.gov.hmrc.agentaccesscontrol.models.AgentReferenceMappings
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentMappingStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testArn
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testSaAgentReference
import uk.gov.hmrc.http.HeaderCarrier

class MappingConnectorISpec extends ComponentSpecHelper with MetricTestSupport with AgentMappingStub {

  val connector: MappingConnector = app.injector.instanceOf[MappingConnector]
  private val saKey: String       = "sa"
  implicit val hc: HeaderCarrier  = HeaderCarrier()

  "MappingConnector" should {
    "return 200 for finding one SA mapping for a particular ARN" in {
      stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
      cleanMetricRegistry()

      await(connector.getAgentMappings(saKey, testArn)) shouldBe AgentReferenceMappings(
        List(AgentReferenceMapping(testArn.value, testSaAgentReference.value))
      )
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentMapping-Check-sa-GET")
    }

    "return 200 for finding multiple SA mappings for a particular ARN" in {
      stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
      cleanMetricRegistry()

      await(connector.getAgentMappings(saKey, testArn)) shouldBe AgentReferenceMappings(
        List(
          AgentReferenceMapping(testArn.value, testSaAgentReference.value),
          AgentReferenceMapping(testArn.value, "A1709A"),
          AgentReferenceMapping(testArn.value, "SA6012")
        )
      )
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentMapping-Check-sa-GET")
    }

    "return 404 for no SA mappings found for a particular ARN" in {
      stubAgentMappingSa(testArn)(NOT_FOUND, Json.obj())
      cleanMetricRegistry()

      await(connector.getAgentMappings(saKey, testArn)) shouldBe AgentReferenceMappings(List.empty)
    }

    "return 400 when requested the wrong / unsupported key" in {
      stubAgentMappingSa(testArn)(BAD_REQUEST, Json.obj())
      cleanMetricRegistry()

      await(connector.getAgentMappings(saKey, testArn)) shouldBe AgentReferenceMappings(List.empty)
    }
  }

}
