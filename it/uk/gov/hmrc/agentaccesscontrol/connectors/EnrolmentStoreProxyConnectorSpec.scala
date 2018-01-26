package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import com.kenshoo.play.metrics.Metrics
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}

import scala.xml.SAXParseException

class EnrolmentStoreProxyConnectorSpec extends WireMockWithOneAppPerSuiteISpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val connector = new EnrolmentStoreProxyConnector(new URL(wiremockBaseUrl), WSHttp, app.injector.instanceOf[Metrics])

  "EnrolmentStoreProxy" should {
    "return sa agent assignments" in {
      given()
        .agentAdmin("AgentCode", "000000123245678900")
          .andIsAssignedToClient(SaUtr("1234567890"))

      val assigned = await(connector.assignedSaAgents(new SaUtr("1234567890")))

      assigned(0).userId shouldBe "000000123245678900"
      assigned(1).userId shouldBe "98741987654321"
      assigned(2).userId shouldBe "98741987654322"
    }

    "return paye agent assignments" in {
      given()
        .agentAdmin("AgentCode", "000000123245678900")
          .andIsAssignedToClient(EmpRef("123", "4567890"))

      val assigned = await(connector.assignedPayeAgents(EmpRef("123", "4567890")))

      assigned(0).userId shouldBe "000000123245678900"
      assigned(1).userId shouldBe "98741987654321"
      assigned(2).userId shouldBe "98741987654322"
    }

    "return empty list if there are no assigned credentials" in {
      given()
        .agentAdmin("AgentCode")
        .andIsNotAllocatedToClient(SaUtr("1234567890"))

      val allocation = await(connector.assignedSaAgents(new SaUtr("1234567890")))

      allocation shouldBe empty
    }

    "throw exception for invalid XML" in {
      given()
        .agentAdmin("AgentCode")
        .andEnrolmentStoreProxyReturnsUnparseableJson(SaUtr("1234567890"))

      an[SAXParseException] should be thrownBy await(connector.assignedSaAgents(new SaUtr("1234567890")))
    }

    "throw exception when HTTP error" in {
      given()
        .agentAdmin("AgentCode")
        .andEnrolmentStoreProxyReturnsAnError500()

      an[Upstream5xxResponse] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
    }

    "record metrics for outbound call" in {
      val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry
      given()
        .agentAdmin("AgentCode")
        .andIsAllocatedAndAssignedToClient(SaUtr("1234567890"))

      await(connector.getAssignedSaAgents(new SaUtr("1234567890")))
      metricsRegistry.getTimers().get("Timer-ConsumedAPI-ESP-ES0-POST").getCount should be >= 1L
    }
  }
}
