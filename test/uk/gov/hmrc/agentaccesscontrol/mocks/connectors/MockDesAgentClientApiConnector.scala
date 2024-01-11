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
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.models.{
  PayeDesAgentClientFlagsApiResponse,
  SaDesAgentClientFlagsApiResponse
}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockDesAgentClientApiConnector extends MockFactory {

  protected val mockDesAgentClientApiConnector: DesAgentClientApiConnector =
    mock[DesAgentClientApiConnector]

  def mockGetSaAgentClientRelationship(
      saAgentReference: SaAgentReference,
      saUtr: SaUtr,
      result: Future[SaDesAgentClientFlagsApiResponse])
    : CallHandler4[SaAgentReference,
                   SaUtr,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[SaDesAgentClientFlagsApiResponse]] = {
    (mockDesAgentClientApiConnector
      .getSaAgentClientRelationship(_: SaAgentReference, _: SaUtr)(
        _: HeaderCarrier,
        _: ExecutionContext))
      .expects(saAgentReference, saUtr, *, *)
      .returning(result)
  }

  def mockGetPayeAgentClientRelationship(
      agentCode: AgentCode,
      empRef: EmpRef,
      result: Future[PayeDesAgentClientFlagsApiResponse])
    : CallHandler4[AgentCode,
                   EmpRef,
                   HeaderCarrier,
                   ExecutionContext,
                   Future[PayeDesAgentClientFlagsApiResponse]] = {
    (mockDesAgentClientApiConnector
      .getPayeAgentClientRelationship(_: AgentCode, _: EmpRef)(
        _: HeaderCarrier,
        _: ExecutionContext))
      .expects(agentCode, empRef, *, *)
      .returning(result)
  }

}
