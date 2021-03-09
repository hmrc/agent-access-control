/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import play.api.{Environment, Logging}
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.service.{
  AuthorisationService,
  ESAuthorisationService
}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorisationController @Inject()(
    val authorisationService: AuthorisationService,
    override val authConnector: AuthConnector,
    val esAuthorisationService: ESAuthorisationService,
    override val env: Environment,
    cc: ControllerComponents)(implicit val ec: ExecutionContext,
                              appConfig: AppConfig)
    extends BackendController(cc)
    with AuthAction
    with Logging {

  val payeEnabled: Boolean = appConfig.featuresPayeAccess

  def isAuthorisedForSa(agentCode: AgentCode,
                        saUtr: SaUtr): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          authorisationService
            .isAuthorisedForSa(agentCode, saUtr, authDetails)
            .map {
              case true  => Ok
              case false => Unauthorized
            }
        }
      }
    }

  def isAuthorisedForMtdIt(agentCode: AgentCode,
                           mtdItId: MtdItId): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          esAuthorisationService
            .authoriseForMtdIt(agentCode, mtdItId, authDetails)
            .map {
              case true  => Ok
              case false => Unauthorized
            }
        }
      }
    }

  def isAuthorisedForMtdVat(agentCode: AgentCode,
                            vrn: Vrn): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          esAuthorisationService
            .authoriseForMtdVat(agentCode, vrn, authDetails)
            .map {
              case true  => Ok
              case false => Unauthorized
            }
        }
      }
    }

  def isAuthorisedForPaye(agentCode: AgentCode,
                          empRef: EmpRef): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          if (payeEnabled) {
            authorisationService.isAuthorisedForPaye(agentCode,
                                                     empRef,
                                                     authDetails) map {
              case true => Ok
              case _    => Unauthorized
            }
          } else {
            logger.warn(s"paye not enabled in configuration")
            Future successful Forbidden
          }
        }
      }
    }

  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          authorisationService.isAuthorisedForAfi(agentCode, nino, authDetails) map {
            isAuthorised =>
              if (isAuthorised) Ok else Unauthorized
          }
        }
      }
    }

  def isAuthorisedForTrust(
      agentCode: AgentCode,
      trustTaxIdentifier: TrustTaxIdentifier): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          esAuthorisationService
            .authoriseForTrust(agentCode, trustTaxIdentifier, authDetails)
            .map {
              case authorised if authorised => Ok
              case _                        => Unauthorized
            }
        }
      }
    }

  def isAuthorisedForCgt(agentCode: AgentCode,
                         cgtRef: CgtRef): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(agentCode) { authDetails =>
        {
          esAuthorisationService
            .authoriseForCgt(agentCode, cgtRef, authDetails)
            .map {
              case authorised if authorised => Ok
              case _                        => Unauthorized
            }
        }
      }
    }
}
