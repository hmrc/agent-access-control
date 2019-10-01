/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.{Request, Result, Results}
import play.api.{Environment, Logger}
import uk.gov.hmrc.agentaccesscontrol.connectors.AuthDetails
import uk.gov.hmrc.agentaccesscontrol.model.{MTD_IT, SA, TaxRegime}
import uk.gov.hmrc.agentaccesscontrol.service.{
  AuthorisationService,
  ESAuthorisationService
}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{
  agentCode,
  allEnrolments,
  credentialRole,
  credentials
}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends AuthorisedFunctions with AuthRedirects with Results {

  def env: Environment

  def authorisationService: AuthorisationService

  def esAuthorisationService: ESAuthorisationService

  def withAgentAuthorisedFor[A](
      regime: TaxRegime,
      ac: AgentCode,
      saUtrOpt: Option[SaUtr] = None,
      mtdItOpt: Option[MtdItId] = None)(body: => Future[Result])(
      implicit request: Request[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[Result] = {
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(agentCode and allEnrolments and credentialRole and credentials) {
        case agentCodeOpt ~ enrols ~ credRole ~ Some(
              Credentials(providerId, _)) => {
          agentCodeOpt match {
            case Some(ac) => {
              val authDetails = fromAuth(enrols, providerId, credRole)
              val authResult: Future[Boolean] = {
                regime match {
                  case SA => {
                    saUtrOpt.fold(Future(false)) { saUtr =>
                      authorisationService
                        .isAuthorisedForSa(AgentCode(ac), saUtr, authDetails)
                    }
                  }
                  case MTD_IT => {
                    mtdItOpt.fold(Future(false)) { mtdId =>
                      esAuthorisationService.authoriseForMtdIt(AgentCode(ac),
                                                               mtdId,
                                                               authDetails)
                    }
                  }
                  case _ => Future(false)
                }
              }
              authResult.flatMap {
                case true  => body
                case false => Future(Unauthorized)
              }
            }
            case None => Future(Forbidden)
          }
        }
        case _ => Future(Forbidden)
      }
      .recover {
        handleException
      }
  }

  private def fromAuth(enrolments: Enrolments,
                       providerId: String,
                       credRole: Option[CredentialRole]): AuthDetails = {
    AuthDetails(saAgentReference = getSaAgentReference(enrolments),
                getArn(enrolments),
                ggCredentialId = providerId,
                affinityGroup = Some("Agent"),
                agentUserRole = credRole)
  }

  private def handleException(
      implicit ec: ExecutionContext,
      request: Request[_]): PartialFunction[Throwable, Result] = {

    case e: UnsupportedAffinityGroup =>
      Logger.warn(s"user did not have the Agent Affinity Group ${e.getMessage}")
      Forbidden

    case e: UnsupportedAuthProvider =>
      Logger.warn(
        s"user was not authorised in Government Gateway ${e.getMessage}")
      Forbidden
  }

  val AsAgentServiceKey = "HMRC-AS-AGENT" // The main Enrolment for MTD Agent Services
  val ArnEnrolmentKey = "AgentReferenceNumber"
  val IRSAServiceKey = "IR-SA-AGENT"
  val IREnrolmentKey = "IRAgentReference"

  private def getArn(enrolments: Enrolments) =
    for {
      enrolment <- enrolments.getEnrolment(AsAgentServiceKey)
      identifier <- enrolment.getIdentifier(ArnEnrolmentKey)
    } yield Arn(identifier.value)

  private def getSaAgentReference(enrolments: Enrolments) =
    for {
      enrolment <- enrolments.getEnrolment(IRSAServiceKey)
      identifier <- enrolment.getIdentifier(IREnrolmentKey)
    } yield SaAgentReference(identifier.value)

}
