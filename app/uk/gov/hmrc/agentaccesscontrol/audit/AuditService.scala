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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.model.MtdClientId
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  import AgentAccessControlEvent.AgentAccessControlEvent

  def auditSaEvent(event: AgentAccessControlEvent,
                 transactionName: String,
                 agentCode: AgentCode,
                 saUtr: SaUtr,
                 details: Seq[(String, Any)] = Seq.empty)
    (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {
    send(createEvent(event, transactionName, agentCode, "sa", saUtr.value, details: _*))
  }

  def auditMtdEvent(event: AgentAccessControlEvent,
                   transactionName: String,
                   agentCode: AgentCode,
                   mtdSaClientId: MtdClientId,
                   details: Seq[(String, Any)] = Seq.empty)
                  (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {
    send(createEvent(event, transactionName, agentCode, "mtd-sa", mtdSaClientId.value, details: _*))
  }

  private def createEvent(event: AgentAccessControlEvent,
                          transactionName: String,
                          agentCode: AgentCode,
                          regime: String,
                          regimeId: String,
                          details: (String, Any)*)
    (implicit hc: HeaderCarrier, request: Request[Any]): DataEvent = {
    DataEvent(
      auditSource = "agent-access-control",
      auditType = event.toString,
      tags = hc.toAuditTags(transactionName, request.path),
      detail = hc.toAuditDetails("agentCode" -> agentCode.value, "regime" -> regime, "regimeId" -> regimeId)
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
  val AgentAccessControlDecision = Value

  type AgentAccessControlEvent = AgentAccessControlEvent.Value
}
