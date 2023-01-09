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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, RequestId, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec

import scala.concurrent.ExecutionContext

class AuditServiceSpec extends UnitSpec with MockitoSugar with Eventually {
  "auditEvent" should {
    "send an event with the correct fields" in {
      val mockConnector = mock[AuditConnector]
      val service = new AuditService(mockConnector)

      val hc = HeaderCarrier(authorization =
                               Some(Authorization("dummy bearer token")),
                             sessionId = Some(SessionId("dummy session id")),
                             requestId = Some(RequestId("dummy request id")))

      service.auditEvent(
        AgentAccessControlDecision,
        "transaction name",
        AgentCode("TESTAGENTCODE"),
        "sa",
        SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail")
      )(hc,
        FakeRequest("GET", "/path"),
        concurrent.ExecutionContext.Implicits.global)

      eventually {
        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(mockConnector).sendEvent(captor.capture())(any[HeaderCarrier],
                                                          any[ExecutionContext])

        val sentEvent = captor.getValue.asInstanceOf[DataEvent]

        sentEvent.auditType shouldBe "AgentAccessControlDecision"
        sentEvent.detail("agentCode") shouldBe "TESTAGENTCODE"
        sentEvent.detail("regime") shouldBe "sa"
        sentEvent.detail("regimeId") shouldBe "TESTSAUTR"
        sentEvent.detail("extra1") shouldBe "first extra detail"
        sentEvent.detail("extra2") shouldBe "second extra detail"
        sentEvent.tags("transactionName") shouldBe "transaction name"
        sentEvent.tags("path") shouldBe "/path"
        sentEvent.tags("X-Session-ID") shouldBe "dummy session id"
        sentEvent.tags("X-Request-ID") shouldBe "dummy request id"
      }
    }
  }

}
