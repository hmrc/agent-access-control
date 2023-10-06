/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentaccesscontrol.model.AccessResponse
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetailsNotFound
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AgentSuspensionChecker { this: Logging =>

  val agentClientAuthorisationConnector: AgentClientAuthorisationConnector

  def withSuspensionCheck(agentId: TaxIdentifier, regime: String)(
      proceed: => Future[AccessResponse])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[AccessResponse] = {

    agentClientAuthorisationConnector
      .getSuspensionDetails(agentId)
      .flatMap {
        case suspensionDetails =>
          val isSuspended = suspensionDetails.suspensionStatus && suspensionDetails.suspendedRegimes
            .contains(regime)
          if (isSuspended) {
            logger.warn(
              s"agent with id : ${agentId.value} is suspended for regime $regime")
            Future.successful(AccessResponse.AgentSuspended)
          } else proceed
      }
      .recover {
        case _: SuspensionDetailsNotFound =>
          val message = s"Suspension details not found for $agentId"
          logger.warn(s"Not authorised: $message")
          AccessResponse.Error(message)
        case e =>
          val message =
            s"Error retrieving suspension details for $agentId: ${e.getMessage}"
          logger.warn(s"Not authorised: $message")
          AccessResponse.Error(message)
      }
  }
}
