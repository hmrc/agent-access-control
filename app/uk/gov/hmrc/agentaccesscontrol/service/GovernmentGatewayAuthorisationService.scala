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

import uk.gov.hmrc.agentaccesscontrol.connectors.GovernmentGatewayProxyConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class GovernmentGatewayAuthorisationService(val ggProxyConnector: GovernmentGatewayProxyConnector) {

  def isAuthorisedInGovernmentGateway(saUtr: SaUtr, ggCredentialId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    ggProxyConnector.getAssignedSaAgents(saUtr) map {
      case _ :: _ :: tail => throw new IllegalStateException(s"More than one agency assigned to $saUtr")
      case agentDetails :: Nil => agentDetails.assignedCredentials.exists(c => c.identifier == ggCredentialId)
      case Nil => false
    }
  }
}
