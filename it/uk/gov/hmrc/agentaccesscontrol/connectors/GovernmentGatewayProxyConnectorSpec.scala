package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaUtr}
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream5xxResponse}

import scala.xml.SAXParseException
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics

class GovernmentGatewayProxyConnectorSpec extends WireMockWithOneAppPerSuiteISpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val connector = new GovernmentGatewayProxyConnector(new URL(wiremockBaseUrl), WSHttp, app.injector.instanceOf[Metrics].defaultRegistry)

  "GovernmentGatewayProxy" should {
    "return sa agent allocations and assignments" in {
      given()
        .agentAdmin("AgentCode", "000000123245678900")
          .andIsAllocatedAndAssignedToClient(SaUtr("1234567890"))

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890")))

      val details: AssignedAgent = allocation.head
      details.allocatedAgentCode shouldBe AgentCode("AgentCode")

      val credentials = details.assignedCredentials.head
      credentials.identifier shouldBe "000000123245678900"

      val credentials1 = details.assignedCredentials(1)
      credentials1.identifier shouldBe "98741987654321"

      val details1: AssignedAgent = allocation(1)
      details1.allocatedAgentCode shouldBe AgentCode("123ABCD12345")

      val credentials2 = details1.assignedCredentials.head
      credentials2.identifier shouldBe "98741987654322"
    }

    "return paye agent allocations and assignments" in {
      given()
        .agentAdmin("AgentCode", "000000123245678900")
          .andIsAllocatedAndAssignedToClient(EmpRef("123", "4567890"))

      val allocation = await(connector.getAssignedPayeAgents(EmpRef("123", "4567890")))

      val details: AssignedAgent = allocation.head
      details.allocatedAgentCode shouldBe AgentCode("AgentCode")

      val credentials = details.assignedCredentials.head
      credentials.identifier shouldBe "000000123245678900"

      val credentials1 = details.assignedCredentials(1)
      credentials1.identifier shouldBe "98741987654321"

      val details1: AssignedAgent = allocation(1)
      details1.allocatedAgentCode shouldBe AgentCode("123ABCD12345")

      val credentials2 = details1.assignedCredentials.head
      credentials2.identifier shouldBe "98741987654322"
    }

    "return empty list if there are no allocated agencies nor assigned credentials" in {
      given()
        .agentAdmin("AgentCode")
        .andIsNotAllocatedToClient(SaUtr("1234567890"))

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890")))

      allocation shouldBe empty
    }

    "throw exception for invalid XML" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayReturnsUnparseableXml(SaUtr("1234567890"))

      an[SAXParseException] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
    }

    "throw exception when HTTP error" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayProxyReturnsAnError500()

      an[Upstream5xxResponse] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
    }

    "record metrics for outbound call" in {
      val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry
      given()
        .agentAdmin("AgentCode")
        .andIsAllocatedAndAssignedToClient(SaUtr("1234567890"))

      await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
      metricsRegistry.getTimers().get("Timer-ConsumedAPI-GGW-GetAssignedAgents-POST").getCount should be >= 1L
    }
  }
}
