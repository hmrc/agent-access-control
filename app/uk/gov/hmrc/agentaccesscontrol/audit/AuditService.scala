/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class AuditService @Inject()(val auditConnector: AuditConnector) {

  def auditEvent(event: AgentAccessControlEvent,
                 transactionName: String,
                 agentCode: AgentCode,
                 regime: String,
                 regimeId: TaxIdentifier,
                 details: Seq[(String, Any)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      request: Request[Any],
      ec: ExecutionContext): Future[Unit] =
    send(
      createEvent(event,
                  transactionName,
                  agentCode,
                  regime,
                  regimeId.value,
                  details: _*))

  private def createEvent(event: AgentAccessControlEvent,
                          transactionName: String,
                          agentCode: AgentCode,
                          regime: String,
                          regimeId: String,
                          details: (String, Any)*)(
      implicit hc: HeaderCarrier,
      request: Request[Any]): DataEvent =
    DataEvent(
      auditSource = "agent-access-control",
      auditType = event.toString,
      tags = hc.toAuditTags(transactionName, request.path),
      detail = hc.toAuditDetails("agentCode" -> agentCode.value,
                                 "regime" -> regime,
                                 "regimeId" -> regimeId)
        ++ Map(details.map(pair => pair._1 -> pair._2.toString): _*)
    )

  private def send(events: DataEvent*)(implicit hc: HeaderCarrier,
                                       ec: ExecutionContext): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendEvent(event))
      }
    }
}

sealed trait AgentAccessControlEvent

case object AgentAccessControlDecision extends AgentAccessControlEvent
