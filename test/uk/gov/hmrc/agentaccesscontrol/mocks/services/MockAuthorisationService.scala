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

import org.scalamock.handlers.CallHandler6
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.models.{AccessResponse, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.services.AuthorisationService
import uk.gov.hmrc.auth.core.{Nino => _}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthorisationService extends MockFactory {

  protected val mockAuthorisationService: AuthorisationService =
    mock[AuthorisationService]

  def mockIsAuthorisedForPaye(
      agentCode: AgentCode,
      empRef: EmpRef,
      authDetails: AuthDetails,
      response: Future[AccessResponse]): CallHandler6[AgentCode,
                                                      EmpRef,
                                                      AuthDetails,
                                                      ExecutionContext,
                                                      HeaderCarrier,
                                                      Request[Any],
                                                      Future[AccessResponse]] =
    (mockAuthorisationService
      .isAuthorisedForPaye(_: AgentCode, _: EmpRef, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(agentCode, empRef, authDetails, *, *, *)
      .returning(response)

  def mockIsAuthorisedForAfi(
      agentCode: AgentCode,
      nino: Nino,
      authDetails: AuthDetails,
      response: Future[AccessResponse]): CallHandler6[AgentCode,
                                                      Nino,
                                                      AuthDetails,
                                                      ExecutionContext,
                                                      HeaderCarrier,
                                                      Request[Any],
                                                      Future[AccessResponse]] =
    (mockAuthorisationService
      .isAuthorisedForAfi(_: AgentCode, _: Nino, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(agentCode, nino, authDetails, *, *, *)
      .returning(response)

  def mockIsAuthorisedForSa(
      agentCode: AgentCode,
      saUtr: SaUtr,
      authDetails: AuthDetails,
      response: Future[AccessResponse]): CallHandler6[AgentCode,
                                                      SaUtr,
                                                      AuthDetails,
                                                      ExecutionContext,
                                                      HeaderCarrier,
                                                      Request[Any],
                                                      Future[AccessResponse]] =
    (mockAuthorisationService
      .isAuthorisedForSa(_: AgentCode, _: SaUtr, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(agentCode, saUtr, authDetails, *, *, *)
      .returning(response)

}
