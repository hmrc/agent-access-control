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

import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors.AuthConnector
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationService(cesaAuthorisationService: CesaAuthorisationService,
                           authConnector: AuthConnector,
                           ggAuthorisationService: GovernmentGatewayAuthorisationService,
                           auditService: AuditService)
  extends LoggingAuthorisationResults {

  def isAuthorised(agentCode: AgentCode, saUtr: SaUtr)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] =
    authConnector.currentAgentIdentifiers().flatMap {
      case Some((Some(saAgentReference), ggCredentialId)) =>
        val results = cesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr) zip
          ggAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, ggCredentialId, saUtr)
        results.map { case (cesa, ggw) => {
          val result = cesa && ggw
          logResult(cesa, ggw, agentCode, ggCredentialId, saUtr, result)
          result
        } }
      case Some((None, _)) =>
        Future successful notAuthorised(s"No 6 digit agent reference found for agent $agentCode")
      case None =>
        Future successful notAuthorised("No user is logged in")
    }

  def logResult(cesa: Boolean, ggw: Boolean, agentCode: AgentCode, ggCredentialId: String, saUtr: SaUtr, result: Boolean)
               (implicit hc: HeaderCarrier) = {
    auditService.auditEvent(AgentAccessControlEvent.AAC_Decision, agentCode, saUtr,
                            Seq("ggCredentialId" -> ggCredentialId, "result" -> result, "cesa" -> cesa, "ggw" -> ggw))
    (cesa, ggw) match {
      case (true, true) => authorised(s"Access allowed for agentCode=$agentCode ggCredential=$ggCredentialId client=$saUtr")
      case (_, _) => notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=$ggCredentialId client=$saUtr cesa=$cesa ggw=$ggw")
    }
  }

}
