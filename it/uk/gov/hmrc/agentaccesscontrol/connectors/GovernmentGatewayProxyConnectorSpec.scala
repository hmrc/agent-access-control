package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import org.mockito.Matchers.any
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito.verify
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.GGW_Response
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream5xxResponse}

import scala.xml.SAXParseException

class GovernmentGatewayProxyConnectorSpec extends BaseISpec with MockitoSugar {

  val auditService = mock[AuditService]
  val agentCode = AgentCode("AgentCode")
  val connector = new GovernmentGatewayProxyConnector(new URL(wiremockBaseUrl), WSHttp, auditService)

  "GovernmentGatewayProxy" should {
    "return agency allocations" in {
      given()
        .agentAdmin("AgentCode")
          .andIsAssignedToClient(SaUtr("1234567890"))

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))

      val details: AssignedAgent = allocation.head
      details.allocatedAgentCode shouldBe AgentCode("AgentCode")

      val details1: AssignedAgent = allocation(1)
      details1.allocatedAgentCode shouldBe AgentCode("123ABCD12345")

      verify(auditService).auditEvent(Matchers.eq(GGW_Response),
                                      Matchers.eq(agentCode),
                                      Matchers.eq(SaUtr("1234567890")),
                                      any[Seq[(String,Any)]])(any[HeaderCarrier])
    }

    "return empty list if there are no matching credentials" in {
      given()
        .agentAdmin("AgentCode")
        .andIsNotAllocatedToClient(SaUtr("1234567890"))

      val allocation = await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))

      allocation shouldBe empty
    }

    "throw exception for invalid XML" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayReturnsUnparseableXml("1234567890")

      an[SAXParseException] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))
    }

    "throw exception when HTTP error" in {
      given()
        .agentAdmin("AgentCode")
        .andGovernmentGatewayProxyReturnsAnError500()

      an[Upstream5xxResponse] should be thrownBy await(connector.getAssignedSaAgents(new SaUtr("1234567890"), agentCode))
    }
  }
}
