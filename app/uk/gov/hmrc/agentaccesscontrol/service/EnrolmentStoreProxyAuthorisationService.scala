/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.service

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentaccesscontrol.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class EnrolmentStoreProxyAuthorisationService @Inject()(
    val enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)
    extends LoggingAuthorisationResults {

  def isAuthorisedForSaInEnrolmentStoreProxy(
      ggCredentialId: String,
      saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[Boolean] =
    getDelegatedAgentUserIdsFor(saUtr) map { assignedAgents =>
      assignedAgents.exists(_.value == ggCredentialId)
    }

  def isAuthorisedForPayeInEnrolmentStoreProxy(
      ggCredentialId: String,
      empRef: EmpRef)(implicit hc: HeaderCarrier): Future[Boolean] =
    enrolmentStoreProxyConnector.getIRPAYEDelegatedUserIdsFor(empRef) map {
      assignedAgents =>
        val result = assignedAgents.exists(_.value == ggCredentialId)
        if (result)
          authorised(
            s"ES0 returned assigned agent credential: $ggCredentialId for client: $empRef")
        else
          notAuthorised(
            s"ES0 did not return assigned agent credential: $ggCredentialId for client $empRef")
    }

  def getDelegatedAgentUserIdsFor(saUtr: SaUtr)(
      implicit hc: HeaderCarrier): Future[Set[AgentUserId]] =
    enrolmentStoreProxyConnector.getIRSADelegatedUserIdsFor(saUtr)

  def getAgentUserIdsFor(saAgentReference: SaAgentReference)(
      implicit hc: HeaderCarrier): Future[Set[AgentUserId]] =
    enrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
      saAgentReference)

  def getAgentUserIdsFor(saAgentReferences: Seq[SaAgentReference])(
      implicit hc: HeaderCarrier)
    : Future[Seq[(SaAgentReference, Set[AgentUserId])]] =
    Future.sequence(
      saAgentReferences.map(r => getAgentUserIdsFor(r).map((r, _))))
}
