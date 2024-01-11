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

import org.scalamock.handlers.{CallHandler3, CallHandler4}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.TaxGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAgentPermissionsConnector extends MockFactory {

  protected val mockAgentPermissionsConnector: AgentPermissionsConnector =
    mock[AgentPermissionsConnector]

  def mockGranularPermissionsOptinRecordExists(arn: Arn, result: Boolean)
    : CallHandler3[Arn, HeaderCarrier, ExecutionContext, Future[Boolean]] = {
    (mockAgentPermissionsConnector
      .granularPermissionsOptinRecordExists(_: Arn)(_: HeaderCarrier,
                                                    _: ExecutionContext))
      .expects(arn, *, *)
      .returns(Future.successful(result))
  }

  def mockGetTaxServiceGroups(
      arn: Arn,
      service: String,
      result: Option[TaxGroup]): CallHandler4[Arn,
                                              String,
                                              HeaderCarrier,
                                              ExecutionContext,
                                              Future[Option[TaxGroup]]] = {
    (mockAgentPermissionsConnector
      .getTaxServiceGroups(_: Arn, _: String)(_: HeaderCarrier,
                                              _: ExecutionContext))
      .expects(arn, service, *, *)
      .returns(Future.successful(result))
  }

}
