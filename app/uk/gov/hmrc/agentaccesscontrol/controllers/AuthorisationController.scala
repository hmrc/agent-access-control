/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.service.AuthorisationService
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.microservice.controller.BaseController




class AuthorisationController(override val auditService: AuditService, authorisationService: AuthorisationService) extends BaseController with Audit {

  def isAuthorised(agentCode: AgentCode, saUtr: SaUtr) = Action.async { implicit request =>
    authorisationService.isAuthorised(agentCode, saUtr).map {
      case authorised if authorised => Ok
      case notAuthorised => Unauthorized
    } recover {
      case _ => Unauthorized // FYI, there's no such fallback in PAYE auth
    }
  }
}

trait Audit {

  val auditService: AuditService
}
