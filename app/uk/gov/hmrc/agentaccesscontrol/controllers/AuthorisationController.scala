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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.Logging
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.CbcId
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.CgtRef
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.MtdItId
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.PlrId
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.PptRef
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.Urn
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.Utr
import uk.gov.hmrc.agentaccesscontrol.models.clientidtypes.Vrn
import uk.gov.hmrc.agentaccesscontrol.models.AccessResponse
import uk.gov.hmrc.agentaccesscontrol.models.Service
import uk.gov.hmrc.agentaccesscontrol.services.AuthorisationService
import uk.gov.hmrc.agentaccesscontrol.services.ESAuthorisationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class AuthorisationController @Inject() (
    val authorisationService: AuthorisationService,
    override val authConnector: AuthConnector,
    val esAuthorisationService: ESAuthorisationService,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with AuthAction
    with Logging {

  def authorise(authType: String, agentCode: String, clientId: String): Action[AnyContent] = {

    Action.async { implicit request: Request[_] =>
      withAgentAuthorised(AgentCode(agentCode)) { authDetails =>
        def standardAuth(service: Service, taxId: TaxIdentifier): Future[AccessResponse] =
          esAuthorisationService.authoriseStandardService(AgentCode(agentCode), taxId, service, authDetails)
        val urnPattern = "^((?i)[a-z]{2}trust[0-9]{8})$"
        val utrPattern = "^\\d{10}$"

        (authType match {
          // Special cases
          case "epaye-auth" =>
            authorisationService.isAuthorisedForPaye(
              AgentCode(agentCode),
              EmpRef.fromIdentifiers(clientId),
              authDetails
            )
          case "sa-auth" =>
            authorisationService.isAuthorisedForSa(AgentCode(agentCode), SaUtr(clientId), authDetails)
          case "afi-auth" =>
            authorisationService.isAuthorisedForAfi(AgentCode(agentCode), Nino(clientId), authDetails)
          // Standard cases
          case "mtd-it-auth"      => standardAuth(Service.MtdIt, MtdItId(clientId))
          case "mtd-it-auth-supp" => standardAuth(Service.MtdItSupp, MtdItId(clientId))
          case "mtd-vat-auth"     => standardAuth(Service.Vat, Vrn(clientId))
          case "trust-auth" if clientId.matches(utrPattern) =>
            standardAuth(Service.Trust, Utr(clientId))
          case "trust-auth" if clientId.matches(urnPattern) =>
            standardAuth(Service.TrustNT, Urn(clientId))
          case "trust-auth" =>
            throw new IllegalArgumentException(
              s"invalid trust tax identifier $clientId"
            ) // TODO this is not caught by the recover
          case "cgt-auth" =>
            standardAuth(Service.CapitalGains, CgtRef(clientId))
          case "ppt-auth" => standardAuth(Service.Ppt, PptRef(clientId))
          case "cbc-auth" =>
            standardAuth(
              Service.Cbc,
              CbcId(clientId)
            ) // AAC does not care about regime for CBC uk or non-uk, handled in ACR - audits will be uk for both
          case "pillar2-auth" => standardAuth(Service.Pillar2, PlrId(clientId))
          case x =>
            throw new IllegalArgumentException(s"Unexpected auth type: $x") // TODO this is not caught by the recover
        }).map {
          case AccessResponse.Authorised   => Ok
          case AccessResponse.NoAssignment => Unauthorized("NO_ASSIGNMENT")
          case _ =>
            Unauthorized("NO_RELATIONSHIP")
        }.recover {
          case e: IllegalArgumentException => BadRequest(e.getMessage)
        }
      }
    }
  }

}
