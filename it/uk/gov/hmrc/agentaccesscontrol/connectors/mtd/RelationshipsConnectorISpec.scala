package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import java.net.URL

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.module.HttpVerbs
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportAppPerSuite, WireMockWithOneAppPerSuiteISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier

class RelationshipsConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val arn = Arn("B1111B")
  implicit val hc = HeaderCarrier()
  val httpVerbs = app.injector.instanceOf[HttpVerbs]

  "relationshipExists for HMRC-MTD-ID" should {
    behave like aCheckEndpoint(MtdItId("C1111C"), "MtdItId")
  }

  "relationshipExists for HMRC-MTD-VAT" should {
    behave like aCheckEndpoint(Vrn("101747641"), "Vrn")
  }

  private def aCheckEndpoint(identifier: TaxIdentifier, clientType: String) = {
    "return true when relationship exists" in new Context {
      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, identifier)) shouldBe true
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }

    "return false when relationship does not exist" in new Context {
      given()
        .mtdAgency(arn)
        .hasNoRelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, identifier)) shouldBe false
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }

    "throw exception when unexpected status code encountered" in new Context {
      given()
        .mtdAgency(arn)
        .statusReturnedForRelationship(identifier, 300)

      intercept[Exception] {
        await(connector.relationshipExists(arn, identifier))
      }.getMessage should include("300")
    }

    //TODO review the need for this auditing - I don't think intra-MDTP calls need to be audited only calls outside MDTP
    "audit the call" in new Context {
      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)

      await(auditConnector.relationshipExists(arn, identifier))

      anOutboundCallShouldBeAudited(arn, identifier)
    }

    "record metrics" in new Context {
      val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, identifier))

      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }
  }

  private abstract class Context extends MockAuditingContext {

    val httpVerbsNew = new HttpVerbs(mockAuditConnector, "", app.injector.instanceOf[Configuration], app.injector.instanceOf[ActorSystem])
    val auditConnector =
      new RelationshipsConnector(new URL(wiremockBaseUrl), httpVerbsNew, FakeMetrics)
    val connector = new RelationshipsConnector(
      new URL(wiremockBaseUrl),
      httpVerbs,
      app.injector.instanceOf[Metrics])

    def anOutboundCallShouldBeAudited(arn: Arn, identifier: TaxIdentifier) = {
      val event = capturedEvent()

      val url = identifier match {
        case _ @MtdItId(mtdItId) =>
          s"$wiremockBaseUrl/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/$mtdItId"
        case _ @Vrn(vrn) =>
          s"$wiremockBaseUrl/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/$vrn"
      }

      event.auditType shouldBe "OutboundCall"

      event.request.tags("path") shouldBe url
    }
  }
}

object FakeMetrics extends Metrics {
  override def defaultRegistry: MetricRegistry = new MetricRegistry
  override def toJson: String = ???
}
