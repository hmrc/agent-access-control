/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.mocks.connectors

import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockACAConnector extends MockFactory {

  protected val mockACAConnector: AgentClientAuthorisationConnector =
    mock[AgentClientAuthorisationConnector]

  def mockGetSuspensionDetails(arn: Arn, result: Future[SuspensionDetails])
    : CallHandler3[Arn,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[SuspensionDetails]] = {
    (mockACAConnector
      .getSuspensionDetails(_: Arn)(_: HeaderCarrier, _: ExecutionContext))
      .expects(arn, *, *)
      .returning(result)
  }

}
