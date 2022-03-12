/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AgentSuspensionChecker { this: LoggingAuthorisationResults =>

  val desAgentClientApiConnector: DesAgentClientApiConnector

  def withSuspensionCheck(isSuspensionEnabled: Boolean,
                          agentId: TaxIdentifier,
                          regime: String)(proceed: => Future[Boolean])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Boolean] = {
    if (isSuspensionEnabled) {
      desAgentClientApiConnector.getAgentRecord(agentId).flatMap {
        case Right(agentRecord) =>
          if (agentRecord.isSuspended && agentRecord.suspendedFor(regime)) {
            logger.warn(
              s"agent with id : ${agentId.value} is suspended for regime $regime")
            Future(false)
          } else {
            proceed
          }
        case Left(message) =>
          logger.warn(message)
          Future(false)
      }
    } else {
      proceed
    }
  }

}
