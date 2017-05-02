package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import java.net.URL

import com.kenshoo.play.metrics.Metrics
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.play.http.HeaderCarrier

class RelationshipsConnectorISpec extends WireMockWithOneAppPerSuiteISpec {

  val arn = Arn("B1111B")
  val client = MtdItId("C1111C")
  implicit val hc = HeaderCarrier()

  "relationshipExists" should {
    "return true when relationship exists" in new Context {
      given().mtdAgency(arn)
        .hasARelationshipWith(client)

      await(connector.relationshipExists(arn, client)) shouldBe true
    }

    "return false when relationship does not exist" in new Context {
      given().mtdAgency(arn)
        .hasNoRelationshipWith(client)

      await(connector.relationshipExists(arn, client)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in new Context {
      given().mtdAgency(arn)
        .statusReturnedForRelationship(client, 300)

      intercept[Exception] {
        await(connector.relationshipExists(arn, client))
      }.getMessage should include("300")
    }

    //TODO review the need for this auditing - I don't think intra-MDTP calls need to be audited only calls outside MDTP
    "audit the call" in new Context {
      given().mtdAgency(arn)
        .hasARelationshipWith(client)

      await(connector.relationshipExists(arn, client))

      anOutboundCallShouldBeAudited(arn, client)
    }

    "record metrics" in new Context {
      val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry

      given().mtdAgency(arn)
        .hasARelationshipWith(client)

      await(connector.relationshipExists(arn, client))

      metricsRegistry.getTimers.get("Timer-ConsumedAPI-RELATIONSHIPS-GetAgentClientRelationship-GET").getCount should be >= 1L
    }
  }

  abstract class Context extends MockAuditingContext {
    def connector = new RelationshipsConnector(new URL(wiremockBaseUrl), wsHttp)

    def anOutboundCallShouldBeAudited(arn: Arn, client: MtdItId) = {
      val event = capturedEvent()

      event.auditType shouldBe "OutboundCall"

      event.request.tags("path") shouldBe s"$wiremockBaseUrl/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${client.value}"
    }
  }
}
