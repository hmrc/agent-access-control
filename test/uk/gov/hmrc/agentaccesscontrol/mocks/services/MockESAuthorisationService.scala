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
import uk.gov.hmrc.agentaccesscontrol.services.ESAuthorisationService
import uk.gov.hmrc.auth.core.{Nino => _}
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockESAuthorisationService extends MockFactory {

  protected val mockESAuthorisationService: ESAuthorisationService =
    mock[ESAuthorisationService]

  def mockAuthoriseStandardService(agentCode: AgentCode,
                                   taxIdentifier: TaxIdentifier,
                                   serviceId: String,
                                   authDetails: AuthDetails,
                                   response: Future[AccessResponse])
    : CallHandler6[AgentCode,
                   TaxIdentifier,
                   String,
                   AuthDetails,
                   HeaderCarrier,
                   Request[Any],
                   Future[AccessResponse]] = {
    (mockESAuthorisationService
      .authoriseStandardService(
        _: AgentCode,
        _: TaxIdentifier,
        _: String,
        _: AuthDetails)(_: HeaderCarrier, _: Request[Any]))
      .expects(agentCode, taxIdentifier, serviceId, authDetails, *, *)
      .returning(response)
  }

}
