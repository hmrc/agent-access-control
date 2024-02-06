package uk.gov.hmrc.agentaccesscontrol.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.helpers.MetricTestSupportAppPerSuite
import uk.gov.hmrc.agentaccesscontrol.helpers.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.agentaccesscontrol.models.AgentReferenceMapping
import uk.gov.hmrc.agentaccesscontrol.models.AgentReferenceMappings
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier

class MappingConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val connector = app.injector.instanceOf[MappingConnector]

  val saAgentReference           = SaAgentReference("enrol-123")
  val arn                        = Arn("AARN0000002")
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "MappingConnector" should {
    "return 200 for finding one SA mapping for a particular ARN" in {
      given().mtdAgency(arn).givenSaMappingSingular("sa", arn, saAgentReference.value)
      givenCleanMetricRegistry()

      await(connector.getAgentMappings("sa", arn)) shouldBe AgentReferenceMappings(
        List(AgentReferenceMapping(arn.value, saAgentReference.value))
      )
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentMapping-Check-sa-GET")
    }

    "return 200 for finding multiple SA mappings for a particular ARN" in {
      given().mtdAgency(arn).givenSaMappingMultiple("sa", arn, saAgentReference.value)
      givenCleanMetricRegistry()

      await(connector.getAgentMappings("sa", arn)) shouldBe AgentReferenceMappings(
        List(
          AgentReferenceMapping(arn.value, saAgentReference.value),
          AgentReferenceMapping(arn.value, "SA6012"),
          AgentReferenceMapping(arn.value, "A1709A")
        )
      )
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentMapping-Check-sa-GET")
    }

    "return 404 for no SA mappings found for a particular ARN" in {
      given().mtdAgency(arn).givenNotFound404Mapping("sa", arn)
      givenCleanMetricRegistry()

      await(connector.getAgentMappings("sa", arn)) shouldBe AgentReferenceMappings(List.empty)
    }

    "return 400 when requested the wrong / unsupported key" in {
      given().mtdAgency(arn).givenBadRequest400Mapping("sa", arn)
      givenCleanMetricRegistry()

      await(connector.getAgentMappings("sa", arn)) shouldBe AgentReferenceMappings(List.empty)
    }
  }

}
