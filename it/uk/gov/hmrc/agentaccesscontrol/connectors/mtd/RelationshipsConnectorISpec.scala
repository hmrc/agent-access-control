package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import java.net.URL

import com.kenshoo.play.metrics.MetricsRegistry
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.model.{Arn, MtdSaClientId}
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.http.HeaderCarrier

class RelationshipsConnectorISpec extends WireMockWithOneAppPerSuiteISpec {

  val agentCode = AgentCode("A1111A")
  val arn = Arn("B1111B")
  val client = MtdSaClientId("C1111C")
  implicit val hc = HeaderCarrier()

  "fetchRelationship" should {
    "audit the call" in new Context {
      given().mtdAgency(agentCode, arn)
        .isAnMtdAgency()
        .andHasARelationshipWith(client)

      await(connector.fetchRelationship(arn, client))

      anOutboundCallShouldBeAudited(arn, client)
    }

    "record metrics" in new Context {
      val metricsRegistry = MetricsRegistry.defaultRegistry

      given().mtdAgency(agentCode, arn)
        .isAnMtdAgency()
        .andHasARelationshipWith(client)

      await(connector.fetchRelationship(arn, client))

      metricsRegistry.getTimers.get("Timer-ConsumedAPI-RELATIONSHIPS-GetAgentClientRelationship-GET").getCount should be >= 1L
    }
  }

  abstract class Context extends MockAuditingContext {
    def connector = new RelationshipsConnector(new URL(wiremockBaseUrl), wsHttp)

    def anOutboundCallShouldBeAudited(arn: Arn, client: MtdSaClientId) = {
      val event = capturedEvent()

      event.auditType shouldBe "OutboundCall"

      event.request.tags("path") shouldBe s"$wiremockBaseUrl/agent-client-relationships/relationships/mtd-sa/${client.value}/${arn.value}"
    }
  }
}
