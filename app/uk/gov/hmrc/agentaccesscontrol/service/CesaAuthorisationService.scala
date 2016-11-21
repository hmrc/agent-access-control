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
import uk.gov.hmrc.agentaccesscontrol.model.{DesAgentClientFlagsApiResponse, FoundResponse, NotFoundResponse}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CesaAuthorisationService(desAgentClientApiConnector: DesAgentClientApiConnector)
  extends LoggingAuthorisationResults {

  def isAuthorisedInCesa(agentCode: AgentCode, saAgentReference: SaAgentReference, saUtr: SaUtr)
    (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    desAgentClientApiConnector
      .getAgentClientRelationship(saAgentReference, saUtr)
      .map(handleDesResponse(agentCode, saUtr, _))
  }

  private def handleDesResponse(agentCode: AgentCode, saUtr: SaUtr, response: DesAgentClientFlagsApiResponse)
                               (implicit headerCarrier: HeaderCarrier): Boolean = {
    response match {
      case NotFoundResponse => {
        notAuthorised(s"DES API returned not found for agent $agentCode and client $saUtr")
      }
      case FoundResponse(true, true) => {
        authorised(s"DES API returned true for both flags for agent $agentCode and client $saUtr")
      }
      case FoundResponse(auth64_8, authI64_8) => {
        notAuthorised(s"DES API returned false for at least one flag agent $agentCode and client $saUtr. 64-8=$auth64_8, i64-8=$authI64_8")
      }
    }
  }
}
