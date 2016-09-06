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

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.GGW_Decision
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.connectors.GovernmentGatewayProxyConnector
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class GovernmentGatewayAuthorisationService(val ggProxyConnector: GovernmentGatewayProxyConnector,
                                            val auditService: AuditService) extends LoggingAuthorisationResults {

  def isAuthorisedInGovernmentGateway(agentCode: AgentCode, ggCredentialId: String, saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[Boolean] = {
    ggProxyConnector.getAssignedSaAgents(saUtr, agentCode) map { assignedAgents =>
      val result = assignedAgents.exists(_.matches(agentCode, ggCredentialId))
      logResult(agentCode, ggCredentialId, saUtr, result)
      result
    }
  }

  def logResult(agentCode: AgentCode, ggCredentialId: String, saUtr: SaUtr, result: Boolean)(implicit hc: HeaderCarrier) = {
    auditService.auditEvent(GGW_Decision, agentCode, saUtr, Seq("ggCredentialId" -> ggCredentialId, "result" -> result))
    result match {
      case true => authorised(s"GGW relationship found for agentCode=$agentCode ggCredential=$ggCredentialId client=$saUtr")
      case false => notAuthorised(s"GGW relationship not found for agentCode=$agentCode ggCredential=$ggCredentialId client=$saUtr")
    }
  }
}
