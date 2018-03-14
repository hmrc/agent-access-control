package uk.gov.hmrc.agentaccesscontrol.connectors.afi

import java.net.URL

import uk.gov.hmrc.agentaccesscontrol.connectors.AfiRelationshipConnector
import uk.gov.hmrc.agentaccesscontrol.support.{ MetricTestSupportAppPerSuite, WireMockWithOneAppPerSuiteISpec }
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.{ AgentCode, Nino }
import uk.gov.hmrc.http.HeaderCarrier

class AfiRelationshipConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val arn = Arn("B1111B")
  val clientId = Nino("AE123456C")
  val agentCode = AgentCode("ABCDEF123456")
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "hasRelationship" should {
    "return true when relationship exists" in new Context {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasRelationship(arn, clientId)

      await(connector.hasRelationship(arn.value, clientId.value)) shouldBe true
    }

    "return false when relationship does not exist" in new Context {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoRelationship(arn, clientId)

      await(connector.hasRelationship(arn.value, clientId.value)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in new Context {
      given().agentAdmin(agentCode).isLoggedIn()
        .statusReturnedForRelationship(arn, clientId, 300)

      intercept[Exception] {
        await(connector.hasRelationship(arn.value, clientId.value))
      }.getMessage should include("300")
    }
  }

  abstract class Context extends MockAuditingContext {
    def connector = app.injector.instanceOf[AfiRelationshipConnector]
  }

}

