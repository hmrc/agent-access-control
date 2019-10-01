//package uk.gov.hmrc.agentaccesscontrol.connectors
//
//import com.fasterxml.jackson.core.JsonParseException
//import com.kenshoo.play.metrics.Metrics
//import org.scalatestplus.mockito.MockitoSugar
//import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
//import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaUtr, TaxIdentifier}
//import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
//
//import scala.concurrent.Future
//
//import concurrent.ExecutionContext.Implicits.global
//
//class EnrolmentStoreProxyConnectorSpec extends WireMockWithOneAppPerSuiteISpec with MockitoSugar {
//
//  implicit val hc: HeaderCarrier = HeaderCarrier()
//
//  val connector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]
//
//  "EnrolmentStoreProxy" when {
//
//    "assignedSaAgents is called" should {
//      behave like anES0Call(connector.getIRSADelegatedUserIdsFor, SaUtr("1234567890"))
//    }
//
//    "assignedPayeAgents is called" should {
//      behave like anES0Call(connector.getIRPAYEDelegatedUserIdsFor, EmpRef("123", "4567890"))
//    }
//
//    def anES0Call[ClientId <: TaxIdentifier](
//      connectorFn: ClientId => Future[Set[AgentUserId]],
//      clientId: ClientId): Unit = {
//      "return agent assignments" in {
//        given()
//          .agentUser("AgentCode", "000000123245678900")
//          .andIsAssignedToClient(clientId, "98741987654321", "98741987654322")
//
//        val assigned = await(connectorFn(clientId)).toSeq
//
//        assigned(0).value shouldBe "98741987654321"
//        assigned(1).value shouldBe "98741987654322"
//        assigned(2).value shouldBe "000000123245678900"
//      }
//
//      "return empty list if there are no assigned credentials" in {
//        given()
//          .agentUser("AgentCode")
//          .andHasNoAssignmentsForAnyClient
//
//        await(connectorFn(clientId)) shouldBe empty
//
//      }
//
//      "return empty list if Enrolment Store Proxy returns 204" in {
//        given()
//          .agentUser("AgentCode")
//          .andEnrolmentStoreProxyReturns204NoContent
//
//        await(connectorFn(clientId)) shouldBe empty
//      }
//
//      "throw exception for invalid JSON" in {
//        given()
//          .agentUser("AgentCode")
//          .andEnrolmentStoreProxyReturnsUnparseableJson(clientId)
//
//        an[JsonParseException] should be thrownBy await(connectorFn(clientId))
//      }
//
//      "throw exception when HTTP error" in {
//        given()
//          .agentUser("AgentCode")
//          .andEnrolmentStoreProxyReturnsAnError500()
//
//        an[Upstream5xxResponse] should be thrownBy await(connectorFn(clientId))
//      }
//
//      "record metrics for outbound call" in {
//        val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry
//        given()
//          .agentUser("AgentCode")
//          .andIsAssignedToClient(clientId)
//
//        await(connectorFn(clientId))
//        metricsRegistry.getTimers().get("Timer-ConsumedAPI-EnrolmentStoreProxy-ES0-GET").getCount should be >= 1L
//      }
//    }
//  }
//}
