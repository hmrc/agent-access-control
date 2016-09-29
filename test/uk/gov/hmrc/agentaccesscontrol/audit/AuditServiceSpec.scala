/*
 * Copyright 2016 HM Revenue & Customs
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
import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{AuditEvent, DataEvent}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class AuditServiceSpec extends UnitSpec with MockitoSugar {
  private implicit val hc = HeaderCarrier()

  "auditEvent" should {
    "send an event with the correct fields" in {
      val mockConnector = mock[AuditConnector]
      val service = new AuditService(mockConnector)

      service.auditEvent(
        AgentAccessControlDecision,
        AgentCode("TESTAGENTCODE"), SaUtr("TESTSAUTR"),
        Seq("extra1" -> "first extra detail", "extra2" -> "second extra detail"))

      val captor = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(mockConnector).sendEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
      captor.getValue shouldBe an[DataEvent]
      val sentEvent = captor.getValue.asInstanceOf[DataEvent]

      sentEvent.auditType shouldBe "AgentAccessControlDecision"
      //TODO should be agentCode to match convention
      sentEvent.detail("agent-code") shouldBe "TESTAGENTCODE"
      sentEvent.detail("regime") shouldBe "sa"
      sentEvent.detail("regimeId") shouldBe "TESTSAUTR"
      sentEvent.detail("extra1") shouldBe "first extra detail"
      sentEvent.detail("extra2") shouldBe "second extra detail"
    }
  }

}
