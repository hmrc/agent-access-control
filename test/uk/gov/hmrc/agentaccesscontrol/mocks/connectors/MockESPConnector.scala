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
import uk.gov.hmrc.agentaccesscontrol.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockESPConnector extends MockFactory {

  protected val mockESPConnector: EnrolmentStoreProxyConnector =
    mock[EnrolmentStoreProxyConnector]

  def mockGetIRSADelegatedUserIdsFor(saUtr: SaUtr,
                                     result: Future[Set[AgentUserId]])
    : CallHandler3[SaUtr,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[Set[AgentUserId]]] = {
    (mockESPConnector
      .getIRSADelegatedUserIdsFor(_: SaUtr)(_: HeaderCarrier,
                                            _: ExecutionContext))
      .expects(saUtr, *, *)
      .returning(result)
  }

  def mockGetIRPAYEDelegatedUserIdsFor(empRef: EmpRef,
                                       result: Future[Set[AgentUserId]])
    : CallHandler3[EmpRef,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[Set[AgentUserId]]] = {
    (mockESPConnector
      .getIRPAYEDelegatedUserIdsFor(_: EmpRef)(_: HeaderCarrier,
                                               _: ExecutionContext))
      .expects(empRef, *, *)
      .returning(result)
  }

  def mockGetIRSAAGENTPrincipalUserIdsFor(saAgentReference: SaAgentReference,
                                          result: Future[Set[AgentUserId]])
    : CallHandler3[SaAgentReference,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[Set[AgentUserId]]] = {
    (mockESPConnector
      .getIRSAAGENTPrincipalUserIdsFor(_: SaAgentReference)(
        _: HeaderCarrier,
        _: ExecutionContext))
      .expects(saAgentReference, *, *)
      .returning(result)
  }

}
