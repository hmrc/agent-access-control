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

import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentaccesscontrol.connectors.MappingConnector
import uk.gov.hmrc.agentaccesscontrol.models.AgentReferenceMappings
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockMappingConnector extends MockFactory {

  protected val mockMappingConnector: MappingConnector = mock[MappingConnector]

  def mockGetAgentMappings(key: String,
                           arn: Arn,
                           response: Future[AgentReferenceMappings])
    : CallHandler4[String,
                   Arn,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[AgentReferenceMappings]] = {
    (mockMappingConnector
      .getAgentMappings(_: String, _: Arn)(_: HeaderCarrier,
                                           _: ExecutionContext))
      .expects(key, arn, *, *)
      .returning(response)
  }

}
