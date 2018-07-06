/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.service

/*
 * Copyright 2018 HM Revenue & Customs
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

import org.mockito.Mockito._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.{AuthConnector, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.support.ResettingMockitoSugar
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.ArgumentMatchers.any

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class MtdVatAuthorisationServiceSpec extends UnitSpec with ResettingMockitoSugar {

  val authConnector = mock[AuthConnector]
  val relationshipsConnector = resettingMock[RelationshipsConnector]
  val auditService = resettingMock[AuditService]

  val service = new MtdVatAuthorisationService(authConnector, relationshipsConnector, auditService)

  val agentCode = AgentCode("agentCode")
  val arn = Arn("arn")
  val vrn = Vrn("vrn")
  implicit val hc = HeaderCarrier()
  implicit val fakeRequest = FakeRequest("GET", "/agent-access-control/mtd-vat-auth/agent/arn/client/vrn")

  "authoriseForMtdVat" should {
    "allow access for agent with a client relationship" in {
      asAgentIsLoggedIn()
      whenRelationshipsConnectorIsCalled thenReturn true

      val result = await(service.authoriseForMtdVat(agentCode, vrn))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      agentWithoutHmrcAsAgentEnrolmentIsLoggedIn()

      val result = await(service.authoriseForMtdVat(agentCode, vrn))

      result shouldBe false
      verify(relationshipsConnector, never)
        .relationshipExists(any[Arn], any[MtdItId])(any[ExecutionContext], any[HeaderCarrier])
    }

    "deny access for a mtd agent without a client relationship" in {
      asAgentIsLoggedIn()
      whenRelationshipsConnectorIsCalled thenReturn false

      val result = await(service.authoriseForMtdVat(agentCode, vrn))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        asAgentIsLoggedIn()
        whenRelationshipsConnectorIsCalled thenReturn true

        await(service.authoriseForMtdVat(agentCode, vrn))

        verify(auditService)
          .auditEvent(
            AgentAccessControlDecision,
            "agent access decision",
            agentCode,
            "mtd-vat",
            vrn,
            Seq("credId" -> "ggId", "accessGranted" -> true, "arn" -> arn.value))(hc, fakeRequest)
      }

      "decision is made to deny access" in {
        asAgentIsLoggedIn()
        whenRelationshipsConnectorIsCalled thenReturn false

        await(service.authoriseForMtdVat(agentCode, vrn))

        verify(auditService)
          .auditEvent(
            AgentAccessControlDecision,
            "agent access decision",
            agentCode,
            "mtd-vat",
            vrn,
            Seq("credId" -> "ggId", "accessGranted" -> false, "arn" -> arn.value))(hc, fakeRequest)
      }

      "no HMRC-AS-AGENT enrolment exists" in {
        agentWithoutHmrcAsAgentEnrolmentIsLoggedIn()

        await(service.authoriseForMtdVat(agentCode, vrn))

        verify(auditService)
          .auditEvent(
            AgentAccessControlDecision,
            "agent access decision",
            agentCode,
            "mtd-vat",
            vrn,
            Seq("credId" -> "ggId", "accessGranted" -> false))(hc, fakeRequest)
      }
    }
  }

  def whenRelationshipsConnectorIsCalled =
    when(relationshipsConnector.relationshipExists(any[Arn], any[MtdItId])(any[ExecutionContext], any[HeaderCarrier]))

  def asAgentIsLoggedIn() =
    when(authConnector.currentAuthDetails()).thenReturn(
      Some(AuthDetails(None, Some(arn), "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))

  def agentWithoutHmrcAsAgentEnrolmentIsLoggedIn() =
    when(authConnector.currentAuthDetails())
      .thenReturn(Some(AuthDetails(None, None, "ggId", affinityGroup = Some("Agent"), agentUserRole = Some("admin"))))
}
