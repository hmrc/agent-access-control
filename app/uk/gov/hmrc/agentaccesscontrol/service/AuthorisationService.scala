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
import uk.gov.hmrc.agentaccesscontrol.connectors.{AuthConnector, AuthDetails}
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationService(cesaAuthorisationService: CesaAuthorisationService,
                           authConnector: AuthConnector,
                           ggAuthorisationService: GovernmentGatewayAuthorisationService,
                           auditService: AuditService)
  extends LoggingAuthorisationResults {

  def isAuthorised(agentCode: AgentCode, saUtr: SaUtr)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] =
    authConnector.currentAuthDetails().flatMap {
      case Some(agentAuthDetails@AuthDetails(Some(saAgentReference), ggCredentialId, _, _)) =>
        val results = cesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr) zip
          ggAuthorisationService.isAuthorisedInGovernmentGateway(agentCode, ggCredentialId, saUtr)
        results.map { case (cesa, ggw) => {
          val result = cesa && ggw
          logResult(agentCode, agentAuthDetails, saUtr, cesa, ggw, result)
          result
        } }
      case Some(AuthDetails(None, _, _, _)) =>
        Future successful notAuthorised(s"No 6 digit agent reference found for agent $agentCode")
      case None =>
        Future successful notAuthorised("No user is logged in")
    }

  private def logResult(
    agentCode: AgentCode, agentAuthDetails: AuthDetails, saUtr: SaUtr,
    cesa: Boolean, ggw: Boolean, result: Boolean)
    (implicit hc: HeaderCarrier) = {

    val optionalDetails = Seq(
      agentAuthDetails.affinityGroup.map("affinityGroup" -> _),
      agentAuthDetails.agentUserRole.map("agentUserRole" -> _)).flatten

    auditService.auditEvent(
      AgentAccessControlEvent.AgentAccessControlDecision, agentCode, saUtr,
      Seq("ggCredentialId" -> agentAuthDetails.ggCredentialId,
        "result" -> result, "cesa" -> cesa, "ggw" -> ggw)
      ++ optionalDetails)

    (cesa, ggw) match {
      case (true, true) =>
        authorised(s"Access allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr")
      case (_, _) =>
        notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr cesa=$cesa ggw=$ggw")
    }
  }

}
