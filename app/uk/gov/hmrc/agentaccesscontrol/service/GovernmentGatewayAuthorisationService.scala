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

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.connectors.GovernmentGatewayProxyConnector
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class GovernmentGatewayAuthorisationService @Inject() (val ggProxyConnector: GovernmentGatewayProxyConnector) extends LoggingAuthorisationResults {

  def isAuthorisedForSaInGovernmentGateway(agentCode: AgentCode, ggCredentialId: String, saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[Boolean] = {
    ggProxyConnector.getAssignedSaAgents(saUtr) map { assignedAgents =>
      assignedAgents.exists(_.matches(agentCode, ggCredentialId))
    }
  }

  def isAuthorisedForPayeInGovernmentGateway(agentCode: AgentCode, ggCredentialId: String, empRef: EmpRef)(implicit hc: HeaderCarrier): Future[Boolean] = {
    ggProxyConnector.getAssignedPayeAgents(empRef) map { assignedAgents =>
      val result = assignedAgents.exists(_.matches(agentCode, ggCredentialId))
      if (result) authorised(s"GGW returned true for assigned agent for $agentCode, GGW credential $ggCredentialId and client $empRef")
      else notAuthorised(s"GGW returned false for assigned agent for $agentCode, GGW credential $ggCredentialId and client $empRef")
    }
  }
}
