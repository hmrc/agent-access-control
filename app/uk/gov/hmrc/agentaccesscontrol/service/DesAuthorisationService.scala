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

import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DesAuthorisationService(desAgentClientApiConnector: DesAgentClientApiConnector)
  extends LoggingAuthorisationResults {

  def isAuthorisedInCesa(agentCode: AgentCode, saAgentReference: SaAgentReference, saUtr: SaUtr)
    (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    desAgentClientApiConnector
      .getSaAgentClientRelationship(saAgentReference, saUtr)
      .map(handleCesaResponse(agentCode, saUtr, _))
  }

  def isAuthorisedInEBS(agentCode: AgentCode, empRef: EmpRef)
    (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    desAgentClientApiConnector
      .getPayeAgentClientRelationship(agentCode, empRef)
      .map(handleEBSResponse(agentCode, empRef, _))

  }

  private def handleCesaResponse(agentCode: AgentCode, saUtr: SaUtr, response: SaDesAgentClientFlagsApiResponse)
                               (implicit headerCarrier: HeaderCarrier): Boolean = {
    response match {
      case SaNotFoundResponse => {
        notAuthorised(s"DES API returned not found for agent $agentCode and client $saUtr")
      }
      case SaFoundResponse(true, true) => {
        authorised(s"DES API returned true for both flags for agent $agentCode and client $saUtr")
      }
      case SaFoundResponse(auth64_8, authI64_8) => {
        notAuthorised(s"DES API returned false for at least one flag agent $agentCode and client $saUtr. 64-8=$auth64_8, i64-8=$authI64_8")
      }
    }
  }

  private def handleEBSResponse(agentCode: AgentCode, empRef: EmpRef, response: PayeDesAgentClientFlagsApiResponse)
                               (implicit headerCarrier: HeaderCarrier): Boolean = {
    response match {
      case PayeNotFoundResponse =>
        notAuthorised(s"DES API returned not found for agent $agentCode and client $empRef")
      case PayeFoundResponse(true, _) =>
        authorised(s"DES API returned true for auth64-8 for agent $agentCode and client $empRef")
      case PayeFoundResponse(false, _) =>
        notAuthorised(s"DES API returned false for auth64-8 flag agent $agentCode and client $empRef")
    }
  }

}
