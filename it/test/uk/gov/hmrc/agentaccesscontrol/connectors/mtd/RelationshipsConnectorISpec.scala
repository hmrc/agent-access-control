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
import uk.gov.hmrc.agentaccesscontrol.models.Service
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentClientRelationshipStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse

class RelationshipsConnectorISpec extends ComponentSpecHelper with AgentClientRelationshipStub {

  implicit val hc: HeaderCarrier        = HeaderCarrier()
  val connector: RelationshipsConnector = app.injector.instanceOf[RelationshipsConnector]

  "relationshipExists for HMRC-MTD-IT" should {
    "return true when relationship exists" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-MTD-IT/client/MTDITID/C1111C",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdIt))
    }
  }

  "relationshipExists for HMRC-MTD-IT-SUPP" should {
    "return true when relationship exists" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(OK)

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-MTD-IT-SUPP/client/MTDITID/C1111C",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubMtdItSuppAgentClientRelationship(testArn, testMtdItId)(OK)

      await(connector.relationshipExists(testArn, None, testMtdItId, Service.MtdItSupp))
    }
  }

  "relationshipExists for HMRC-MTD-VAT" should {
    "return true when relationship exists" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)

      await(connector.relationshipExists(testArn, None, testVrn, Service.Vat)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testVrn, Service.Vat)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testVrn, Service.Vat))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-MTD-VAT/client/VRN/101747641",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)

      await(connector.relationshipExists(testArn, None, testVrn, Service.Vat))
    }
  }

  "relationshipExists for HMRC-TERS-ORG" should {
    "return true when relationship exists" in {
      stubTersAgentClientRelationship(testArn, testUtr)(OK)

      await(connector.relationshipExists(testArn, None, testUtr, Service.Trust)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubTersAgentClientRelationship(testArn, testUtr)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testUtr, Service.Trust)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubTersAgentClientRelationship(testArn, testUtr)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testUtr, Service.Trust))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-TERS-ORG/client/SAUTR/5066836985",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubTersAgentClientRelationship(testArn, testUtr)(OK)

      await(connector.relationshipExists(testArn, None, testUtr, Service.Trust))
    }
  }

  "relationshipExists for HMRC-TERSNT-ORG" should {
    "return true when relationship exists" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)

      await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-TERSNT-ORG/client/URN/XATRUST06683698",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)

      await(connector.relationshipExists(testArn, None, testUrn, Service.TrustNT))
    }
  }

  "relationshipExists for HMRC-CGT-PD" should {
    "return true when relationship exists" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)

      await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-CGT-PD/client/CGTPDRef/XMCGTP123456789",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)

      await(connector.relationshipExists(testArn, None, testCgtRef, Service.CapitalGains))
    }
  }

  "relationshipExists for HMRC-PPT-ORG" should {
    "return true when relationship exists" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)

      await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-PPT-ORG/client/EtmpRegistrationNumber/XHPPT0006633194",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)

      await(connector.relationshipExists(testArn, None, testPptRef, Service.Ppt))
    }
  }

  "relationshipExists for HMRC-CBC-ORG or HMRC-CBC-NONUK-ORG" should {
    "return true when relationship exists" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(OK)

      await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-CBC-ORG/client/cbcId/XHCBC0006633194",
        MULTIPLE_CHOICES
      )
    }

    "record metrics" in {
      stubCbcIdAgentClientRelationship(testArn, testCbcId)(OK)

      await(connector.relationshipExists(testArn, None, testCbcId, Service.Cbc))
    }
  }

  "relationshipExists for HMRC-PILLAR2" should {
    "return true when relationship exists" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(OK)

      await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(NOT_FOUND)

      await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(MULTIPLE_CHOICES)

      val exception = the[UpstreamErrorResponse] thrownBy {
        await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2))
      }

      exception shouldBe UpstreamErrorResponse(
        "Error calling: http://localhost:11111/agent-client-relationships/agent/01234567890/service/HMRC-PILLAR2-ORG/client/PLRID/XDPLR6210917659",
        MULTIPLE_CHOICES,
        MULTIPLE_CHOICES,
        Map()
      )
    }

    "record metrics" in {
      stubPlrIdAgentClientRelationship(testArn, testPlrId)(OK)

      await(connector.relationshipExists(testArn, None, testPlrId, Service.Pillar2))
    }
  }

}
