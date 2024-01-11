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

package uk.gov.hmrc.agentaccesscontrol.mocks.services

import org.scalamock.handlers.{CallHandler3, CallHandler4}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentaccesscontrol.services.EnrolmentStoreProxyAuthorisationService
import uk.gov.hmrc.auth.core.{Nino => _}
import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockESPAuthorisationService extends MockFactory {

  protected val mockESPAuthorisationService
    : EnrolmentStoreProxyAuthorisationService =
    mock[EnrolmentStoreProxyAuthorisationService]

  def mockIsAuthorisedForPayeInEnrolmentStoreProxy(
      ggCredentialId: String,
      empRef: EmpRef,
      response: Future[Boolean]): CallHandler4[String,
                                               EmpRef,
                                               HeaderCarrier,
                                               ExecutionContext,
                                               Future[Boolean]] = {
    (mockESPAuthorisationService
      .isAuthorisedForPayeInEnrolmentStoreProxy(_: String, _: EmpRef)(
        _: HeaderCarrier,
        _: ExecutionContext))
      .expects(ggCredentialId, empRef, *, *)
      .returning(response)
  }

  def mockIsAuthorisedForSaInEnrolmentStoreProxy(
      ggCredentialId: String,
      saUtr: SaUtr,
      response: Future[Boolean]): CallHandler4[String,
                                               SaUtr,
                                               HeaderCarrier,
                                               ExecutionContext,
                                               Future[Boolean]] = {
    (mockESPAuthorisationService
      .isAuthorisedForSaInEnrolmentStoreProxy(_: String, _: SaUtr)(
        _: HeaderCarrier,
        _: ExecutionContext))
      .expects(ggCredentialId, saUtr, *, *)
      .returning(response)
  }

  def mockGetDelegatedAgentUserIdsFor(saUtr: SaUtr,
                                      response: Future[Set[AgentUserId]])
    : CallHandler3[SaUtr,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[Set[AgentUserId]]] = {
    (mockESPAuthorisationService
      .getDelegatedAgentUserIdsFor(_: SaUtr)(_: HeaderCarrier,
                                             _: ExecutionContext))
      .expects(saUtr, *, *)
      .returning(response)
  }

  def mockGetAgentUserIdsFor(
      agentReferences: Seq[SaAgentReference],
      response: Future[Seq[(SaAgentReference, Set[AgentUserId])]])
    : CallHandler3[Seq[SaAgentReference],
                   HeaderCarrier,
                   ExecutionContext,
                   Future[Seq[(SaAgentReference, Set[AgentUserId])]]] = {
    (mockESPAuthorisationService
      .getAgentUserIdsFor(_: Seq[SaAgentReference])(_: HeaderCarrier,
                                                    _: ExecutionContext))
      .expects(agentReferences, *, *)
      .returning(response)
  }

}
