/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.controllers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.mvc.Result
import play.api.mvc.Results
import play.api.Logging
import uk.gov.hmrc.agentaccesscontrol.models.AuthDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.agentCode
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentialRole
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier

trait AuthAction extends AuthorisedFunctions with Results with Logging {

  def withAgentAuthorised[A](
      ac: AgentCode
  )(body: AuthDetails => Future[Result])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    authorised(AuthProviders(GovernmentGateway).and(AffinityGroup.Agent))
      .retrieve(agentCode.and(allEnrolments).and(credentialRole).and(credentials)) {
        case agentCodeOpt ~ enrols ~ credRole ~ Some(Credentials(providerId, _)) =>
          agentCodeOpt match {
            case Some(agentCode) if agentCode == ac.value =>
              body(
                AuthDetails(
                  saAgentReference = getSaAgentReference(enrols),
                  getArn(enrols),
                  ggCredentialId = providerId,
                  affinityGroup = Some("Agent"),
                  agentUserRole = credRole
                )
              )
            case Some(_) =>
              logger.warn(s"agent code from auth did not match the agent code in url")
              Future(Forbidden)
            case None =>
              logger.info(s"no agent code found in auth details for agent code $ac")
              Future(Forbidden)
          }
        case err => throw new Exception(s"Authorisation retrieval error: $err")
      }
      .recover {
        handleException()
      }
  }

  private def handleException(): PartialFunction[Throwable, Result] = {
    case e: UnsupportedAffinityGroup =>
      logger.warn(s"user did not have the Agent Affinity Group ${e.getMessage}")
      Forbidden

    case e: UnsupportedAuthProvider =>
      logger.warn(s"user was not authorised in Government Gateway ${e.getMessage}")
      Forbidden
  }

  private val AsAgentServiceKey = "HMRC-AS-AGENT" // The main Enrolment for MTD Agent Services
  private val ArnEnrolmentKey   = "AgentReferenceNumber"
  private val IRSAServiceKey    = "IR-SA-AGENT"
  private val IREnrolmentKey    = "IRAgentReference"

  private def getArn(enrolments: Enrolments) =
    for {
      enrolment  <- enrolments.getEnrolment(AsAgentServiceKey)
      identifier <- enrolment.getIdentifier(ArnEnrolmentKey)
    } yield Arn(identifier.value)

  private def getSaAgentReference(enrolments: Enrolments) =
    for {
      enrolment  <- enrolments.getEnrolment(IRSAServiceKey)
      identifier <- enrolment.getIdentifier(IREnrolmentKey)
    } yield SaAgentReference(identifier.value)

}
