package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.SaUtr

class GovernmentGatewayProxyConnectorSpec extends BaseISpec {

  val connector = new GovernmentGatewayProxyConnector(new URL(wiremockBaseUrl), WSHttp)

  "GovernmentGatewayProxy" should {
    "return agent allocations" in {
      given()
        .agentAdmin("AgentCode")
          .andIsAssignedToClient("1234567890")

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890")))

      val details: AgentDetails = allocation.get
      details.agentCode shouldBe "AgentCode"

      val credentials = details.assignedCredentials.head
      credentials.identifier shouldBe "0000001232456789"

      val credentials1 = details.assignedCredentials(1)
      credentials1.identifier shouldBe "98741987654321"
    }
  }
}
