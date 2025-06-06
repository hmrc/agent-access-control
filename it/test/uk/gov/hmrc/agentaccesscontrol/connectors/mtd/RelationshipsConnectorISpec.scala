/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentClientRelationshipStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._
import uk.gov.hmrc.agentmtdidentifiers.model.Service
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse

class RelationshipsConnectorISpec extends ComponentSpecHelper with MetricTestSupport with AgentClientRelationshipStub {

  implicit val hc: HeaderCarrier        = HeaderCarrier()
  val connector: RelationshipsConnector = app.injector.instanceOf[RelationshipsConnector]

  "relationshipExists for HMRC-MTD-IT" should {
    "return true when relationship exists" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckMtdItId-GET")
    }

    "return false when relationship does not exist" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckMtdItId-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckMtdItId-GET")
    }
  }

  "relationshipExists for HMRC-MTD-IT-SUPP" should {
    "return true when relationship exists" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckMtdItId-GET")
    }

    "return false when relationship does not exist" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckMtdItId-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckMtdItId-GET")
    }
  }

  "relationshipExists for HMRC-MTD-VAT" should {
    "return true when relationship exists" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testVrn, Service.Vat)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckVrn-GET")
    }

    "return false when relationship does not exist" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testVrn, Service.Vat)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckVrn-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testVrn, Service.Vat))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testVrn, Service.Vat))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckVrn-GET")
    }
  }

  "relationshipExists for HMRC-TERS-ORG" should {
    "return true when relationship exists" in {
      stubTersAgentClientRelationship(testArn, testUtr)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testUtr, Service.Trust)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckUtr-GET")
    }

    "return false when relationship does not exist" in {
      stubTersAgentClientRelationship(testArn, testUtr)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testUtr, Service.Trust)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckUtr-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubTersAgentClientRelationship(testArn, testUtr)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testUtr, Service.Trust))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubTersAgentClientRelationship(testArn, testUtr)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testUtr, Service.Trust))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckUtr-GET")
    }
  }

  "relationshipExists for HMRC-TERSNT-ORG" should {
    "return true when relationship exists" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckUrn-GET")
    }

    "return false when relationship does not exist" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckUrn-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckUrn-GET")
    }
  }

  "relationshipExists for HMRC-CGT-PD" should {
    "return true when relationship exists" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckCgtRef-GET")
    }

    "return false when relationship does not exist" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckCgtRef-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckCgtRef-GET")
    }
  }

  "relationshipExists for HMRC-PPT-ORG" should {
    "return true when relationship exists" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckPptRef-GET")
    }

    "return false when relationship does not exist" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckPptRef-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckPptRef-GET")
    }
  }

  "relationshipExists for HMRC-CBC-ORG or HMRC-CBC-NONUK-ORG" should {
    "return true when relationship exists" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckCbcId-GET")
    }

    "return false when relationship does not exist" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckCbcId-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckCbcId-GET")
    }
  }

  "relationshipExists for HMRC-PILLAR2" should {
    "return true when relationship exists" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2)) shouldBe true
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckPlrId-GET")
    }

    "return false when relationship does not exist" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(NOT_FOUND)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2)) shouldBe false
      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckPlrId-GET")
    }

    "throw exception when unexpected status code encountered" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(MULTIPLE_CHOICES)
      cleanMetricRegistry()

      intercept[UpstreamErrorResponse] {
        await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2))
      }.statusCode shouldBe 300
    }

    "record metrics" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(OK)
      cleanMetricRegistry()

      await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2))

      timerShouldExistAndHasBeenUpdated(s"ConsumedAPI-AgentClientRelationships-CheckPlrId-GET")
    }
  }

}
