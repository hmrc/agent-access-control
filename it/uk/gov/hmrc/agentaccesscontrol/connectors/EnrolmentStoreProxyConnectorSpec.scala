package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import com.fasterxml.jackson.core.JsonParseException
import com.kenshoo.play.metrics.Metrics
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.domain.{EmpRef, SaUtr, TaxIdentifier}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends WireMockWithOneAppPerSuiteISpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val connector = new EnrolmentStoreProxyConnector(new URL(wiremockBaseUrl), WSHttp, app.injector.instanceOf[Metrics])

  "EnrolmentStoreProxy" when {

    "assignedSaAgents is called" should {
      behave like anES0Call(connector.assignedSaAgents, SaUtr("1234567890"))
    }

    "assignedPayeAgents is called" should {
      behave like anES0Call(connector.assignedPayeAgents, EmpRef("123", "4567890"))
    }

    def anES0Call[ClientId <: TaxIdentifier](connectorFn: ClientId => Future[Seq[AssignedAgentCredentials]], clientId: ClientId): Unit = {
      "return agent assignments" in {
        given()
          .agentAdmin("AgentCode", "000000123245678900")
          .andIsAssignedToClient(clientId)

        val assigned = await(connectorFn(clientId))

        assigned(0).userId shouldBe "000000123245678900"
        assigned(1).userId shouldBe "98741987654321"
        assigned(2).userId shouldBe "98741987654322"
      }

      "return empty list if there are no assigned credentials" in {
        given()
          .agentAdmin("AgentCode")
            .andHasNoAssignmentsForAnyClient

        val allocation = await(connectorFn(clientId))

        allocation shouldBe empty
      }

      "throw exception for invalid JSON" in {
        given()
          .agentAdmin("AgentCode")
            .andEnrolmentStoreProxyReturnsUnparseableJson(clientId)

        an[JsonParseException] should be thrownBy await(connectorFn(clientId))
      }

      "throw exception when HTTP error" in {
        given()
          .agentAdmin("AgentCode")
            .andEnrolmentStoreProxyReturnsAnError500()

        an[Upstream5xxResponse] should be thrownBy await(connectorFn(clientId))
      }

      "record metrics for outbound call" in {
        val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry
        given()
          .agentAdmin("AgentCode")
            .andIsAssignedToClient(clientId)

        await(connectorFn(clientId))
        metricsRegistry.getTimers().get("Timer-ConsumedAPI-EnrolmentStoreProxy-ES0-GET").getCount should be >= 1L
      }
    }
  }
}
