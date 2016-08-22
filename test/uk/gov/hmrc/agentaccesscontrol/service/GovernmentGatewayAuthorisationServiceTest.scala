package uk.gov.hmrc.agentaccesscontrol.service

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.connectors.{AgentDetails, AssignedCredentials, GovernmentGatewayProxyConnector}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class GovernmentGatewayAuthorisationServiceTest extends UnitSpec with MockitoSugar {

  val ggProxyConnector = mock[GovernmentGatewayProxyConnector]
  val service = new GovernmentGatewayAuthorisationService(ggProxyConnector)
  val utr = new SaUtr("0123456789")
  implicit val hc = new HeaderCarrier()

  "isAuthorisedInGovernmentGateway " should {
    "return true if the agent is assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Some(AgentDetails("AgentCode", Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(utr, "000111333"))

      result shouldBe true
    }

    "return false if the agent is not assigned to the client" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenReturn(Future successful Some(AgentDetails("AgentCode", Seq(AssignedCredentials("000111333")))))

      val result = await(service.isAuthorisedInGovernmentGateway(utr, "NonMatchingCred"))

      result shouldBe false
    }

    "throw exception if government gateway proxy fails" in {
      when(ggProxyConnector.getAssignedSaAgents(utr)(hc)).thenThrow(new RuntimeException())

      an[RuntimeException] should be thrownBy await(service.isAuthorisedInGovernmentGateway(utr, "NonMatchingCred"))
    }
  }
}
