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

import org.scalamock.handlers.{CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentaccesscontrol.services.DesAuthorisationService
import uk.gov.hmrc.auth.core.{Nino => _}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockDesAuthorisationService extends MockFactory {

  protected val mockDesAuthorisationService: DesAuthorisationService =
    mock[DesAuthorisationService]

  def mockIsAuthorisedInEbs(
      agentCode: AgentCode,
      empRef: EmpRef,
      response: Future[Boolean]): CallHandler4[AgentCode,
                                               EmpRef,
                                               ExecutionContext,
                                               HeaderCarrier,
                                               Future[Boolean]] = {
    (mockDesAuthorisationService
      .isAuthorisedInEbs(_: AgentCode, _: EmpRef)(_: ExecutionContext,
                                                  _: HeaderCarrier))
      .expects(agentCode, empRef, *, *)
      .returning(response)
  }

  def mockIsAuthorisedInCesa(
      agentCode: AgentCode,
      saAgentReference: SaAgentReference,
      saUtr: SaUtr,
      response: Future[Boolean]): CallHandler5[AgentCode,
                                               SaAgentReference,
                                               SaUtr,
                                               ExecutionContext,
                                               HeaderCarrier,
                                               Future[Boolean]] = {
    (mockDesAuthorisationService
      .isAuthorisedInCesa(_: AgentCode, _: SaAgentReference, _: SaUtr)(
        _: ExecutionContext,
        _: HeaderCarrier))
      .expects(agentCode, saAgentReference, saUtr, *, *)
      .returning(response)
  }

}
