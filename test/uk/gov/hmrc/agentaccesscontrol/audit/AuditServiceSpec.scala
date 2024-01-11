/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.audit

import play.api.test.FakeRequest
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitTest
import uk.gov.hmrc.agentaccesscontrol.mocks.connectors.MockAuditConnector
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{
  Success,
  Failure,
  Disabled
}

class AuditServiceSpec extends UnitTest with MockAuditConnector {

  "createAuditEvent" should {
    "create an event with the correct fields" in {
      val service = new AuditService(mockAuditConnector)

      val hc = HeaderCarrier(authorization =
                               Some(Authorization("dummy bearer token")),
                             sessionId = Some(SessionId("dummy session id")),
                             requestId = Some(RequestId("dummy request id")))

      val result = service.createAuditEvent(
        event = AgentAccessControlDecision,
        transactionName = "transaction name",
        agentCode = AgentCode("TESTAGENTCODE"),
        regime = "sa",
        regimeId = "TESTSAUTR",
        details = "extra1" -> "first extra detail",
        "extra2" -> "second extra detail"
      )(hc, FakeRequest("GET", "/path"))

      result.auditType shouldBe "AgentAccessControlDecision"
      result.detail("agentCode") shouldBe "TESTAGENTCODE"
      result.detail("regime") shouldBe "sa"
      result.detail("regimeId") shouldBe "TESTSAUTR"
      result.detail("extra1") shouldBe "first extra detail"
      result.detail("extra2") shouldBe "second extra detail"
      result.tags("transactionName") shouldBe "transaction name"
      result.tags("path") shouldBe "/path"
      result.tags("X-Session-ID") shouldBe "dummy session id"
      result.tags("X-Request-ID") shouldBe "dummy request id"
    }
  }

  "sendAuditEvent" should {
    "handle a success response from the audit connector" in {
      val service = new AuditService(mockAuditConnector)
      mockSendEvent(Success)

      val hc = HeaderCarrier(authorization =
                               Some(Authorization("dummy bearer token")),
                             sessionId = Some(SessionId("dummy session id")),
                             requestId = Some(RequestId("dummy request id")))

      val result = service.sendAuditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      await(result) shouldBe Success
    }

    "handle a failure response from the audit connector" in {
      val service = new AuditService(mockAuditConnector)
      mockSendEvent(Failure("error"))

      val hc = HeaderCarrier(authorization =
                               Some(Authorization("dummy bearer token")),
                             sessionId = Some(SessionId("dummy session id")),
                             requestId = Some(RequestId("dummy request id")))

      val result = service.sendAuditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      await(result) shouldBe Failure("error")
    }

    "handle a disabled response from the audit connector" in {
      val service = new AuditService(mockAuditConnector)
      mockSendEvent(Disabled)

      val hc = HeaderCarrier(authorization =
                               Some(Authorization("dummy bearer token")),
                             sessionId = Some(SessionId("dummy session id")),
                             requestId = Some(RequestId("dummy request id")))

      val result = service.sendAuditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      await(result) shouldBe Disabled
    }
  }

}
