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

import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{AgentAccessControlEvent, AuditService}
import uk.gov.hmrc.agentaccesscontrol.connectors._
import uk.gov.hmrc.agentaccesscontrol.model.PossibleAgentRefsToCreds
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AuthorisationService @Inject() (
  desAuthorisationService: DesAuthorisationService,
  authConnector: AuthConnector,
  espAuthorisationService: EnrolmentStoreProxyAuthorisationService,
  auditService: AuditService,
  mappingConnector: MappingConnector,
  afiRelationshipConnector: AfiRelationshipConnector)
  extends LoggingAuthorisationResults {

  private val accessGranted = true
  private val accessDenied = false

  def isAuthorisedForSa(agentCode: AgentCode, saUtr: SaUtr)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] =
    authConnector.currentAuthDetails().flatMap {
      case Some(agentAuthDetails @ AuthDetails(Some(saAgentReference), _, _, _, _)) =>
        checkDelegatedSa(agentCode, saUtr, agentAuthDetails, saAgentReference, None)
      case Some(agentAuthDetails @ AuthDetails(_, Some(arn), _, _, _)) =>
        for {
          saAgentReferences <- mappingConnector.getAgentMappings("sa", arn).map(_.mappings.map(m => SaAgentReference(m.identifier)))
          credentials <- espAuthorisationService.getActiveGGCredential(saAgentReferences)
          pairs <- possiblePairs(saAgentReferences , credentials)
          authorised <- checkDelegatedSa(agentCode, saUtr, agentAuthDetails, pairs)
        } yield authorised
      case Some(agentAuthDetails @ AuthDetails(None, _, _, _, _)) =>
        auditDecision(agentCode, agentAuthDetails, "sa", saUtr, result = false, None)
        Future successful notAuthorised(s"No 6 digit agent reference found for agent $agentCode")
      case None =>
        Future successful notAuthorised("No user is logged in")
    }

  private def possiblePairs(saList: List[SaAgentReference], credList: Seq[AssignedAgentCredentials]) = {
    val pairs = for {
      sa <- saList
      cred <- credList
    } yield PossibleAgentRefsToCreds(sa, cred)
    Future successful pairs
  }

  def checkDelegatedSa(agentCode: AgentCode,
                       saUtr: SaUtr,
                       agentAuthDetails: AuthDetails,
                       possibleAgentRefsToCredsList: List[PossibleAgentRefsToCreds])
                      (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    possibleAgentRefsToCredsList match {
      case pairCred::pairCreds =>
        checkDelegatedSa(agentCode, saUtr, agentAuthDetails, pairCred.saAgentReference, Some(pairCred.assignedAgentCredentials.userId)).flatMap {
        case true => Future successful true
        case false =>
          Logger.warn(s"$agentCode Combination given is not authorised")
          checkDelegatedSa(agentCode, saUtr, agentAuthDetails, pairCreds)
      }
      case Nil =>
        Logger.warn(s"No pairs found for $agentCode")
        Future successful false
    }
  }

  def checkDelegatedSa(agentCode: AgentCode, saUtr: SaUtr, agentAuthDetails: AuthDetails, saAgentReference: SaAgentReference, oldGGCredential: Option[String])(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    for {
      isAuthorisedInESP <- espAuthorisationService.isAuthorisedForSaInEnrolmentStoreProxy(oldGGCredential.getOrElse(agentAuthDetails.ggCredentialId), saUtr)
      maybeCesa <- checkCesaIfNecessary(isAuthorisedInESP, agentCode, saAgentReference, saUtr)
    } yield {
      val result = isAuthorisedInESP && maybeCesa.get

      val cesaDescription = desResultDescription(maybeCesa)
      auditDecision(
        agentCode,
        agentAuthDetails,
        "sa",
        saUtr,
        result,
        oldGGCredential,
        "cesaResult" -> cesaDescription,
        "enrolmentStoreResult" -> isAuthorisedInESP)

      if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${oldGGCredential.getOrElse(agentAuthDetails.ggCredentialId)} client=$saUtr")
      else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${oldGGCredential.getOrElse(agentAuthDetails.ggCredentialId)} client=$saUtr esp=$isAuthorisedInESP cesa=$cesaDescription")
    }
  }

  private def checkCesaIfNecessary(isAuthorisedInESP: Boolean, agentCode: AgentCode, saAgentReference: SaAgentReference, saUtr: SaUtr)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    if (isAuthorisedInESP) desAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr).map(Some.apply)
    else Future successful None

  def isAuthorisedForPaye(agentCode: AgentCode, empRef: EmpRef)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] =
    authConnector.currentAuthDetails().flatMap {
      case Some(agentAuthDetails @ AuthDetails(_, _, ggCredentialId, _, _)) =>
        for {
          isAuthorisedInESP <- espAuthorisationService.isAuthorisedForPayeInEnrolmentStoreProxy(ggCredentialId, empRef)
          maybeEbs <- checkEbsIfNecessary(isAuthorisedInESP, agentCode, empRef)
        } yield {
          val result = isAuthorisedInESP && maybeEbs.get

          val ebsDescription = desResultDescription(maybeEbs)
          auditDecision(agentCode, agentAuthDetails, "paye", empRef, result, None, "ebsResult" -> ebsDescription, "enrolmentStoreResult" -> isAuthorisedInESP)

          if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$empRef")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$empRef esp=$isAuthorisedInESP ebs=$ebsDescription")
        }
      case None => Future successful notAuthorised("No user is logged in")
    }

  private def checkEbsIfNecessary(isAuthorisedInESP: Boolean, agentCode: AgentCode, empRef: EmpRef)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    if (isAuthorisedInESP) desAuthorisationService.isAuthorisedInEbs(agentCode, empRef).map(Some.apply)
    else Future successful None

  private def desResultDescription(maybeEbs: Option[Boolean]): Any = {
    maybeEbs.getOrElse("notChecked")
  }

  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    authConnector.currentAuthDetails().flatMap {
      case Some(authDetails @ AuthDetails(_, Some(arn), _, _, _)) =>
        val arnValue = arn.value
        afiRelationshipConnector.hasRelationship(arnValue, nino.value) map { hasRelationship =>
          if (hasRelationship) {
            auditDecision(agentCode, authDetails, "afi", nino, accessGranted, None, "" -> "")
            found("Relationship Found")
          } else {
            auditDecision(agentCode, authDetails, "afi", nino, accessDenied, None, "" -> "")
            notFound("No relationship found")
          }
        }
      case _ => Future successful notFound("Error retrieving arn")
    }
  }

  private def auditDecision(
    agentCode: AgentCode,
    agentAuthDetails: AuthDetails,
    regime: String,
    taxIdentifier: TaxIdentifier,
    result: Boolean,
    oldGGCredential: Option[String],
    extraDetails: (String, Any)*)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {
    val optionalDetails = Seq(
      agentAuthDetails.saAgentReference.map("saAgentReference" -> _),
      agentAuthDetails.affinityGroup.map("affinityGroup" -> _),
      agentAuthDetails.agentUserRole.map("agentUserRole" -> _),
      oldGGCredential.map("oldGGCredential" -> _)).flatten

    auditService.auditEvent(
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      regime,
      taxIdentifier,
      Seq(
        "credId" -> agentAuthDetails.ggCredentialId,
        "accessGranted" -> result)
        ++ extraDetails
        ++ optionalDetails)
  }
}
