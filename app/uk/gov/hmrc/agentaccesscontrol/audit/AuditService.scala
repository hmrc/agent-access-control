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

import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class AuditService(val auditConnector: AuditConnector) {

  import AgentAccessControlEvent.AgentAccessControlEvent

  def auditEvent(event: AgentAccessControlEvent,
                 agentCode: AgentCode,
                 saUtr: SaUtr,
                 details: Seq[(String, Any)] = Seq.empty)(implicit hc: HeaderCarrier): Future[Unit] = {
    send(createEvent(event, agentCode, saUtr, details: _*))
  }

  private def createEvent(event: AgentAccessControlEvent,
                          agentCode: AgentCode,
                          saUtr: SaUtr,
                          details: (String, Any)*)(implicit hc: HeaderCarrier): DataEvent = {
    DataEvent(
      auditSource = "agent-access-control",
      auditType = event.toString,
      tags = hc.headers.toMap,
      detail = Map("agent-code" -> agentCode.toString, "sa-utr" -> saUtr.toString())
               ++ Map(details.map(pair => pair._1 -> pair._2.toString): _*)
    )
  }

  private def send(events: DataEvent*)(implicit hc: HeaderCarrier): Future[Unit] = {
    Future {
      events.foreach { event =>
        Try(auditConnector.sendEvent(event))
      }
    }
  }
}

object AgentAccessControlEvent extends Enumeration {
  val CESA_Decision,AgentAccessControlDecision = Value

  type AgentAccessControlEvent = AgentAccessControlEvent.Value
}
