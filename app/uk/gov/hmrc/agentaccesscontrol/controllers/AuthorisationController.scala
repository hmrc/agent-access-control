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
import play.api.Configuration
import play.api.mvc.Action
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, MtdItAuthorisationService, MtdVatAuthorisationService}
import uk.gov.hmrc.agentmtdidentifiers.model.{MtdItId, Vrn}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

@Singleton
class AuthorisationController @Inject()(
  override val auditService: AuditService,
  authorisationService: AuthorisationService,
  mtdItAuthorisationService: MtdItAuthorisationService,
  mtdVatAuthorisationService: MtdVatAuthorisationService,
  configuration: Configuration,
  ecp: Provider[ExecutionContextExecutor])
    extends BaseController with Audit {

  implicit val ec: ExecutionContext = ecp.get

  def isAuthorisedForSa(agentCode: AgentCode, saUtr: SaUtr) = Action.async { implicit request =>
    authorisationService.isAuthorisedForSa(agentCode, saUtr).map {
      case authorised if authorised => Ok
      case notAuthorised            => Unauthorized
    }
  }

  def isAuthorisedForMtdIt(agentCode: AgentCode, mtdItId: MtdItId) =
    Action.async { implicit request =>
      mtdItAuthorisationService.authoriseForMtdIt(agentCode, mtdItId) map {
        case authorised if authorised => Ok
        case notAuthorised            => Unauthorized
      }
    }

  def isAuthorisedForMtdVat(agentCode: AgentCode, vrn: Vrn) = Action.async { implicit request =>
    mtdVatAuthorisationService.authoriseForMtdVat(agentCode, vrn) map {
      case authorised if authorised => Ok
      case notAuthorised            => Unauthorized
    }
  }

  def isAuthorisedForPaye(agentCode: AgentCode, empRef: EmpRef) = Action.async { implicit request =>
    val payeEnabled: Boolean =
      configuration.getBoolean("features.allowPayeAccess").getOrElse(false)

    if (payeEnabled) {
      authorisationService.isAuthorisedForPaye(agentCode, empRef) map {
        case true => Ok
        case _    => Unauthorized
      }
    } else {
      Future(Forbidden)
    }
  }

  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino) = Action.async { implicit request =>
    authorisationService.isAuthorisedForAfi(agentCode, nino) map { isAuthorised =>
      if (isAuthorised) Ok else NotFound
    }
  }
}

trait Audit {
  val auditService: AuditService
}
