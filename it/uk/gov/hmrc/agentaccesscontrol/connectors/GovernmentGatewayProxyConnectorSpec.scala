package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.Upstream5xxResponse

import scala.xml.SAXParseException

class GovernmentGatewayProxyConnectorSpec extends BaseISpec {

  val connector = new GovernmentGatewayProxyConnector(new URL(wiremockBaseUrl), WSHttp)

  "GovernmentGatewayProxy" should {
    "return agent allocations" in {
      given()
        .agentAdmin("AgentCode")
          .andIsAssignedToClient(SaUtr("1234567890"))

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890")))

      val details: AgentDetails = allocation.get
      details.agentCode shouldBe "AgentCode"

      val credentials = details.assignedCredentials.head
      credentials.identifier shouldBe "0000001232456789"

      val credentials1 = details.assignedCredentials(1)
      credentials1.identifier shouldBe "98741987654321"
    }

    "return None if there are no matching credentials" in {
      given()
        .agentAdmin("AgentCode")
        .andIsNotAssignedToClient(SaUtr("1234567890"))

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890")))

      allocation shouldBe None
    }

    "throw exception for invalid XML" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayReturnsUnparseableXml("1234567890")

      an[SAXParseException] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
    }

    "throw exception when HTTP error" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayProxyReturnsAnError500()

      an[Upstream5xxResponse] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
    }
  }
}
