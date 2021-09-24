package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportAppPerSuite, WireMockWithOneAppPerSuiteISpec}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class RelationshipsConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val arn = Arn("B1111B")
  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[RelationshipsConnector]

  "relationshipExists for HMRC-MTD-IT" should {
    behave like aCheckEndpoint(MtdItId("C1111C"), "MtdItId")
  }

  "relationshipExists for HMRC-MTD-VAT" should {
    behave like aCheckEndpoint(Vrn("101747641"), "Vrn")
  }

  "relationshipExists for HMRC-TERS-ORG" should {
    behave like aCheckEndpoint(Utr("101747641"), "Utr")
  }

  "relationshipExists for HMRC-TERSNT-ORG" should {
    behave like aCheckEndpoint(Urn("urn101747641"), "Urn")
  }

  "relationshipExists for HMRC-CGT-PD" should {
    behave like aCheckEndpoint(CgtRef("XMCGTP123456789"), "CgtRef")
  }

  private def aCheckEndpoint(identifier: TaxIdentifier, clientType: String) = {
    "return true when relationship exists" in {
      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, identifier)) shouldBe true
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }

    "return false when relationship does not exist" in {
      given()
        .mtdAgency(arn)
        .hasNoRelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, identifier)) shouldBe false
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }

    "throw exception when unexpected status code encountered" in {
      given()
        .mtdAgency(arn)
        .statusReturnedForRelationship(identifier, 300)

      intercept[Exception] {
        await(connector.relationshipExists(arn, identifier))
      }.getMessage should include("300")
    }

    "record metrics" in {
      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, identifier))

      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }
  }
}
