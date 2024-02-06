package uk.gov.hmrc.agentaccesscontrol.connectors.afi

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.connectors.AfiRelationshipConnector
import uk.gov.hmrc.agentaccesscontrol.helpers.MetricTestSupportAppPerSuite
import uk.gov.hmrc.agentaccesscontrol.helpers.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

class AfiRelationshipConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val arn        = Arn("B1111B")
  val clientId   = Nino("AE123456C")
  val agentCode  = AgentCode("ABCDEF123456")
  val providerId = "12345-credId"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector = app.injector.instanceOf[AfiRelationshipConnector]

  "hasRelationship" should {
    "return true when relationship exists" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .andHasRelationship(arn, clientId)

      await(connector.hasRelationship(arn.value, clientId.value)) shouldBe true
    }

    "return false when relationship does not exist" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .andHasNoRelationship(arn, clientId)

      await(connector.hasRelationship(arn.value, clientId.value)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .statusReturnedForRelationship(arn, clientId, 300)

      intercept[Exception] {
        await(connector.hasRelationship(arn.value, clientId.value))
      }.getMessage should include("300")
    }
  }
}
