/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.{AuthConnector, AuthDetails}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MtdItAuthorisationService @Inject() (authConnector: AuthConnector,
                              relationshipsConnector: RelationshipsConnector,
                              auditService: AuditService) extends LoggingAuthorisationResults {

  def authoriseForMtdIt(agentCode: AgentCode, mtdItId: MtdItId)
                    (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[_]): Future[Boolean] = {
    authConnector.currentAuthDetails() flatMap  {
      case Some(agentAuthDetails@AuthDetails(_, Some(arn), _, _, _)) => hasRelationship(arn, mtdItId) map { result =>
        auditDecision(agentCode, agentAuthDetails, mtdItId, result, "arn" -> arn.value)
        if (result) authorised(s"Access allowed for agentCode=$agentCode arn=${arn.value} client=${mtdItId.value}")
        else notAuthorised(s"Access not allowed for agentCode=$agentCode arn=${arn.value} client=${mtdItId.value}")
      }
      case Some(agentAuthDetails) =>
        auditDecision(agentCode, agentAuthDetails, mtdItId, result = false)
        Future successful notAuthorised(s"No ARN found in HMRC-AS-AGENT enrolment for agentCode $agentCode")
      case None => Future successful notAuthorised("No user is logged in")
    }
  }

  private def hasRelationship(arn: Arn, mtdItId: MtdItId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    relationshipsConnector.relationshipExists(arn, mtdItId)
  }

  private def auditDecision(
                             agentCode: AgentCode, agentAuthDetails: AuthDetails, mtdItId: MtdItId,
                             result: Boolean, extraDetails: (String, Any)*)
                           (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {

    auditService.auditEvent(
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      "mtd-it",
      mtdItId,
      Seq("credId" -> agentAuthDetails.ggCredentialId,
        "accessGranted" -> result)
        ++ extraDetails)
  }
}
