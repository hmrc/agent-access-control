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

import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors.{AfiRelationshipConnector, AuthConnector, AuthDetails}
import uk.gov.hmrc.domain._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class AuthorisationService @Inject()(desAuthorisationService: DesAuthorisationService,
                                     authConnector: AuthConnector,
                                     ggAuthorisationService: GovernmentGatewayAuthorisationService,
                                     auditService: AuditService,
                                     afiRelationshipConnector: AfiRelationshipConnector)
  extends LoggingAuthorisationResults {

  private val accessGranted = true
  private val accessDenied = false

  def isAuthorisedForSa(agentCode: AgentCode, saUtr: SaUtr)
                       (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] =
    authConnector.currentAuthDetails().flatMap {
      case Some(agentAuthDetails@AuthDetails(Some(saAgentReference), _, ggCredentialId, _, _)) =>
        for {
          ggw <- ggAuthorisationService.isAuthorisedForSaInGovernmentGateway(agentCode, ggCredentialId, saUtr)
          maybeCesa <- checkCesaIfNecessary(ggw, agentCode, saAgentReference, saUtr)
        } yield {
          val result = ggw && maybeCesa.get

          val cesaDescription = desResultDescription(maybeCesa)
          auditDecision(agentCode, agentAuthDetails, "sa", saUtr, result, "cesaResult" -> cesaDescription, "gatewayResult" -> ggw)

          if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr ggw=$ggw cesa=$cesaDescription")
        }
      case Some(agentAuthDetails@AuthDetails(None, _, _, _, _)) =>
        auditDecision(agentCode, agentAuthDetails, "sa", saUtr, result = false)
        Future successful notAuthorised(s"No 6 digit agent reference found for agent $agentCode")
      case None =>
        Future successful notAuthorised("No user is logged in")
    }

  private def checkCesaIfNecessary(ggw: Boolean, agentCode: AgentCode, saAgentReference: SaAgentReference, saUtr: SaUtr)
                                  (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    if (ggw) desAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr).map(Some.apply)
    else Future successful None


  def isAuthorisedForPaye(agentCode: AgentCode, empRef: EmpRef)
                         (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] =
    authConnector.currentAuthDetails().flatMap {
      case Some(agentAuthDetails@AuthDetails(_, _, ggCredentialId, _, _)) =>
        for {
          ggw <- ggAuthorisationService.isAuthorisedForPayeInGovernmentGateway(agentCode, ggCredentialId, empRef)
          maybeEbs <- checkEbsIfNecessary(ggw, agentCode, empRef)
        } yield {
          val result = ggw && maybeEbs.get

          val ebsDescription = desResultDescription(maybeEbs)
          auditDecision(agentCode, agentAuthDetails, "paye", empRef, result, "ebsResult" -> ebsDescription, "gatewayResult" -> ggw)

          if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$empRef")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$empRef ggw=$ggw ebs=$ebsDescription")
        }
      case None => Future successful notAuthorised("No user is logged in")
    }

  private def checkEbsIfNecessary(ggw: Boolean, agentCode: AgentCode, empRef: EmpRef)
                                 (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    if (ggw) desAuthorisationService.isAuthorisedInEbs(agentCode, empRef).map(Some.apply)
    else Future successful None

  private def desResultDescription(maybeEbs: Option[Boolean]): Any = {
    maybeEbs.getOrElse("notChecked")
  }

  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino)
                        (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    authConnector.currentAuthDetails().flatMap {
      case Some(authDetails@AuthDetails(_, Some(arn), _, _, _)) =>
        val arnValue = arn.value
        afiRelationshipConnector.hasRelationship(arnValue, nino.value) map { hasRelationship =>
          if (hasRelationship) {
            auditDecision(agentCode, authDetails, "afi", nino, accessGranted, "" -> "")
            found("Relationship Found")
          } else {
            auditDecision(agentCode, authDetails, "afi", nino, accessDenied, "" -> "")
            notFound("No relationship found")
          }
        }
      case _ => Future successful notFound("Error retrieving arn")
    }
  }


  private def auditDecision(
                             agentCode: AgentCode, agentAuthDetails: AuthDetails, regime: String, taxIdentifier: TaxIdentifier,
                             result: Boolean, extraDetails: (String, Any)*)
                           (implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {
    val optionalDetails = Seq(
      agentAuthDetails.saAgentReference.map("saAgentReference" -> _),
      agentAuthDetails.affinityGroup.map("affinityGroup" -> _),
      agentAuthDetails.agentUserRole.map("agentUserRole" -> _)).flatten

    auditService.auditEvent(
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      regime,
      taxIdentifier,
      Seq("credId" -> agentAuthDetails.ggCredentialId,
        "accessGranted" -> result)
        ++ extraDetails
        ++ optionalDetails)
  }
}
