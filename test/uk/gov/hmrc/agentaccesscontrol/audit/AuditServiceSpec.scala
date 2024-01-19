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

import org.mockito.ArgumentMatchersSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{
  Disabled,
  Failure,
  Success
}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends UnitSpec with ArgumentMatchersSugar {

  trait Setup {
    protected val mockAuditConnector: AuditConnector = mock[AuditConnector]

    object TestService extends AuditService(mockAuditConnector)
  }

  "createAuditEvent" should {
    "create an event with the correct fields" in new Setup {
      val hc: HeaderCarrier =
        HeaderCarrier(authorization = Some(Authorization("dummy bearer token")),
                      sessionId = Some(SessionId("dummy session id")),
                      requestId = Some(RequestId("dummy request id")))

      val result: DataEvent = TestService.createAuditEvent(
        event = AgentAccessControlDecision,
        transactionName = "transaction name",
        agentCode = AgentCode("TESTAGENTCODE"),
        regime = "sa",
        regimeId = "TESTSAUTR",
        details = "extra1" -> "first extra detail",
        "extra2" -> "second extra detail"
      )(hc, FakeRequest("GET", "/path"))

      result.auditType mustBe "AgentAccessControlDecision"
      result.detail("agentCode") mustBe "TESTAGENTCODE"
      result.detail("regime") mustBe "sa"
      result.detail("regimeId") mustBe "TESTSAUTR"
      result.detail("extra1") mustBe "first extra detail"
      result.detail("extra2") mustBe "second extra detail"
      result.tags("transactionName") mustBe "transaction name"
      result.tags("path") mustBe "/path"
      result.tags("X-Session-ID") mustBe "dummy session id"
      result.tags("X-Request-ID") mustBe "dummy request id"
    }
  }

  "sendAuditEvent" should {
    "handle a success response from the audit connector" in new Setup {
      mockAuditConnector.sendEvent(any[DataEvent])(*[HeaderCarrier], *[ExecutionContext]) returns Future
        .successful(Success)

      val hc: HeaderCarrier =
        HeaderCarrier(authorization = Some(Authorization("dummy bearer token")),
                      sessionId = Some(SessionId("dummy session id")),
                      requestId = Some(RequestId("dummy request id")))

      val result: Future[AuditResult] = TestService.sendAuditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      await(result) mustBe Success
    }

    "handle a failure response from the audit connector" in new Setup {
      mockAuditConnector.sendEvent(any[DataEvent])(
        any[HeaderCarrier],
        any[ExecutionContext]) returns Future.successful(Failure("error"))

      val hc: HeaderCarrier =
        HeaderCarrier(authorization = Some(Authorization("dummy bearer token")),
                      sessionId = Some(SessionId("dummy session id")),
                      requestId = Some(RequestId("dummy request id")))

      val result: Future[AuditResult] = TestService.sendAuditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      await(result) mustBe Failure("error")
    }

    "handle a disabled response from the audit connector" in new Setup {
      mockAuditConnector.sendEvent(any[DataEvent])(
        any[HeaderCarrier],
        any[ExecutionContext]) returns Future.successful(Disabled)

      val hc: HeaderCarrier =
        HeaderCarrier(authorization = Some(Authorization("dummy bearer token")),
                      sessionId = Some(SessionId("dummy session id")),
                      requestId = Some(RequestId("dummy request id")))

      val result: Future[AuditResult] = TestService.sendAuditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      await(result) mustBe Disabled
    }
  }

}
