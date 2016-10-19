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

package uk.gov.hmrc.agentaccesscontrol.service

import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.{AgenciesConnector, RelationshipsConnector}
import uk.gov.hmrc.agentaccesscontrol.model.{Arn, MtdSaClientId}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MtdAuthorisationService(agenciesConnector: AgenciesConnector,
                              relationshipsConnector: RelationshipsConnector,
                              auditService: AuditService) extends LoggingAuthorisationResults {

  def authoriseForSa(agentCode: AgentCode, mtdSaClientId: MtdSaClientId)
                    (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[_]): Future[Boolean] = {
    agenciesConnector.fetchAgencyRecord(agentCode) flatMap  {
      case Some(agency) => hasRelationship(agency.arn, mtdSaClientId) map { result =>
        auditDecision(agentCode, mtdSaClientId, result, "arn" -> agency.arn)
        if (result) authorised(s"Access allowed for agentCode=$agentCode client=${mtdSaClientId.value}")
        else notAuthorised(s"Access not allowed for agentCode=$agentCode client=${mtdSaClientId.value}")
      }
      case None =>
        auditDecision(agentCode, mtdSaClientId, result = false)
        Future successful notAuthorised(s"No MTD agency record for agentCode $agentCode")
    }
  }

  private def hasRelationship(arn: Arn, mtdSaClientId: MtdSaClientId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    relationshipsConnector.fetchRelationship(arn, mtdSaClientId) map { _.isDefined }
  }

  private def auditDecision(
                             agentCode: AgentCode, mtdSaClientId: MtdSaClientId,
                             result: Boolean, extraDetails: (String, Any)*)
                           (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {

    auditService.auditMtdEvent(
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode, mtdSaClientId,
      Seq("accessGranted" -> result)
        ++ extraDetails)
  }
}
