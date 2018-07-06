package uk.gov.hmrc.agentaccesscontrol.connectors

import uk.gov.hmrc.agentaccesscontrol.model.{AgentReferenceMapping, AgentReferenceMappings}
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportAppPerSuite, WireMockWithOneAppPerSuiteISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class MappingConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val connector = app.injector.instanceOf[MappingConnector]

  val arn = Arn("AARN0000002")
  implicit val hc = HeaderCarrier()

  "MappingConnector" should {
    "return 200 for finding one SA mapping for a particular ARN" in {
      given().mtdAgency(arn).givenSaMappingSingular("sa", arn)
      givenCleanMetricRegistry()

      await(connector.getAgentMappings("sa", arn)) shouldBe AgentReferenceMappings(
        List(AgentReferenceMapping(arn.value, "ABC456")))
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentMapping-Check-sa-GET")
    }

    "return 200 for finding multiple SA mappings for a particular ARN" in {
      given().mtdAgency(arn).givenSaMappingMultiple("sa", arn)
      givenCleanMetricRegistry()

      await(connector.getAgentMappings("sa", arn)) shouldBe AgentReferenceMappings(
        List(
          AgentReferenceMapping(arn.value, "ABC456"),
          AgentReferenceMapping(arn.value, "SA6012"),
          AgentReferenceMapping(arn.value, "A1709A")))
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
