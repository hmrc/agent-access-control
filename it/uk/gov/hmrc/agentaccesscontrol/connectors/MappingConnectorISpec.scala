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
