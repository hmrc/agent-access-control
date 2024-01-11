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

import org.scalamock.handlers.CallHandler9
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlEvent,
  AuditService
}
import uk.gov.hmrc.auth.core.{Nino => _}
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import scala.concurrent.{ExecutionContext, Future}

trait MockAuditService extends MockFactory {

  protected val mockAuditService: AuditService =
    mock[AuditService]

  def mockSendAuditEvent: CallHandler9[AgentAccessControlEvent,
                                       String,
                                       AgentCode,
                                       String,
                                       TaxIdentifier,
                                       List[(String, Any)],
                                       HeaderCarrier,
                                       Request[Any],
                                       ExecutionContext,
                                       Future[AuditResult]] = {
    (
      mockAuditService
        .sendAuditEvent(_: AgentAccessControlEvent,
                        _: String,
                        _: AgentCode,
                        _: String,
                        _: TaxIdentifier,
                        _: List[(String, Any)])(
          _: HeaderCarrier,
          _: Request[Any],
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *, *, *, *)
      .atLeastOnce()
      .returning(Future.successful(Success))
  }

}
