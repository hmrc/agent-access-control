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

import javax.inject.{Inject, Provider, Singleton}
import play.api.{Configuration, Environment}
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.agentaccesscontrol.model.{MTD_IT, SA}
import uk.gov.hmrc.agentaccesscontrol.service.{
  AuthorisationService,
  ESAuthorisationService
}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

@Singleton
class AuthorisationController @Inject()(
    override val authorisationService: AuthorisationService,
    override val authConnector: AuthConnector,
    override val esAuthorisationService: ESAuthorisationService,
    override val config: Configuration,
    override val env: Environment,
    ecp: Provider[ExecutionContextExecutor])
    extends BaseController
    with AuthAction {

  implicit val ec: ExecutionContext = ecp.get

  def isAuthorisedForSa(agentCode: AgentCode,
                        saUtr: SaUtr): Action[AnyContent] =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorisedFor(regime = SA,
                             ac = agentCode,
                             saUtrOpt = Some(saUtr)) { Future successful Ok }
    }

  def isAuthorisedForMtdIt(agentCode: AgentCode, mtdItId: MtdItId) =
    Action.async { implicit request: Request[_] =>
      withAgentAuthorisedFor(regime = MTD_IT,
                             ac = agentCode,
                             mtdItOpt = Some(mtdItId)) { Future successful Ok }
    }

//  def isAuthorisedForMtdVat(agentCode: AgentCode, vrn: Vrn) = Action.async {
//    implicit request =>
//      esAuthorisationService.authoriseForMtdVat(agentCode, vrn) map {
//        case authorised if authorised => Ok
//        case _                        => Unauthorized
//      }
//  }

//  def isAuthorisedForPaye(agentCode: AgentCode, empRef: EmpRef) = Action.async {
//    implicit request =>
//      val payeEnabled: Boolean =
//        configuration.getBoolean("features.allowPayeAccess").getOrElse(false)
//
//      if (payeEnabled) {
//        authorisationService.isAuthorisedForPaye(agentCode, empRef) map {
//          case true => Ok
//          case _    => Unauthorized
//        }
//      } else {
//        Future(Forbidden)
//      }
//  }

//  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino) = Action.async {
//    implicit request =>
//      authorisationService.isAuthorisedForAfi(agentCode, nino) map {
//        isAuthorised =>
//          if (isAuthorised) Ok else Unauthorized
//      }
//  }

//  def isAuthorisedForTrust(agentCode: AgentCode, utr: Utr): Action[AnyContent] =
//    Action.async { implicit request =>
//      esAuthorisationService.authoriseForTrust(agentCode, utr).map {
//        case authorised if authorised => Ok
//        case _                        => Unauthorized
//      }
//    }
}
