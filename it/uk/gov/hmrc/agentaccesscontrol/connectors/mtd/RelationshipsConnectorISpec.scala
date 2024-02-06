package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.helpers.MetricTestSupportAppPerSuite
import uk.gov.hmrc.agentaccesscontrol.helpers.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

class RelationshipsConnectorISpec extends WireMockWithOneAppPerSuiteISpec with MetricTestSupportAppPerSuite {

  val arn: Arn                          = Arn("B1111B")
  implicit val hc: HeaderCarrier        = HeaderCarrier()
  val connector: RelationshipsConnector = app.injector.instanceOf[RelationshipsConnector]

  "relationshipExists for HMRC-MTD-IT" should {
    behave.like(aCheckEndpoint(MtdItId("C1111C"), "MtdItId"))
  }

  "relationshipExists for HMRC-MTD-VAT" should {
    behave.like(aCheckEndpoint(Vrn("101747641"), "Vrn"))
  }

  "relationshipExists for HMRC-TERS-ORG" should {
    behave.like(aCheckEndpoint(Utr("101747641"), "Utr"))
  }

  "relationshipExists for HMRC-TERSNT-ORG" should {
    behave.like(aCheckEndpoint(Urn("urn101747641"), "Urn"))
  }

  "relationshipExists for HMRC-CGT-PD" should {
    behave.like(aCheckEndpoint(CgtRef("XMCGTP123456789"), "CgtRef"))
  }

  "relationshipExists for HMRC-PPT-ORG" should {
    behave.like(aCheckEndpoint(PptRef("XHPPT0006633194"), "PptRef"))
  }

  "relationshipExists for HMRC-CBC-ORG or HMRC-CBC-NONUK-ORG" should {
    behave.like(aCheckEndpoint(CbcId("XHCBC0006633194"), "CbcId"))
  }

  private def aCheckEndpoint(identifier: TaxIdentifier, clientType: String): Unit = {
    "return true when relationship exists" in {
      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, None, identifier)) shouldBe true
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }

    "return false when relationship does not exist" in {
      given()
        .mtdAgency(arn)
        .hasNoRelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, None, identifier)) shouldBe false
      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }

    "throw exception when unexpected status code encountered" in {
      given()
        .mtdAgency(arn)
        .statusReturnedForRelationship(identifier, 300)

      intercept[Exception] {
        await(connector.relationshipExists(arn, None, identifier))
      }.getMessage should include("300")
    }

    "record metrics" in {
      given()
        .mtdAgency(arn)
        .hasARelationshipWith(identifier)
      givenCleanMetricRegistry()

      await(connector.relationshipExists(arn, None, identifier))

      timerShouldExistsAndBeenUpdated(s"ConsumedAPI-AgentClientRelationships-Check$clientType-GET")
    }
  }
}
