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

import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{ AgentAccessControlEvent, AuditService }
import uk.gov.hmrc.agentaccesscontrol.connectors._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

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
        authoriseNonMtdAgentForIRSA(agentCode, saUtr, agentAuthDetails, saAgentReference)
      case Some(agentAuthDetails @ AuthDetails(_, Some(arn), _, _, _)) =>
        authoriseMtdAgentForIRSA(agentCode, saUtr, agentAuthDetails, arn)
      case Some(agentAuthDetails @ AuthDetails(None, _, _, _, _)) =>
        auditDecision(agentCode, agentAuthDetails, "sa", saUtr, result = false)
        Future successful notAuthorised(s"No 6 digit agent reference found for agent $agentCode")
      case None =>
        Future successful notAuthorised("No user is logged in")
    }

  //noinspection ScalaStyle
  def authoriseNonMtdAgentForIRSA(agentCode: AgentCode, saUtr: SaUtr, agentAuthDetails: AuthDetails, saAgentReference: SaAgentReference)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    for {
      isAuthorisedInESP <- espAuthorisationService.isAuthorisedForSaInEnrolmentStoreProxy(agentAuthDetails.ggCredentialId, saUtr)
      maybeCesa <- if (isAuthorisedInESP) desAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr).map(Some.apply)
      else Future successful None
    } yield {
      val result = isAuthorisedInESP && maybeCesa.get

      val cesaDescription = desResultDescription(maybeCesa)
      auditDecision(
        agentCode,
        agentAuthDetails,
        "sa",
        saUtr,
        result,
        "cesaResult" -> cesaDescription,
        "enrolmentStoreResult" -> isAuthorisedInESP)

      if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr")
      else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr esp=$isAuthorisedInESP cesa=$cesaDescription")
    }
  }

  //noinspection ScalaStyle
  def authoriseMtdAgentForIRSA(agentCode: AgentCode, saUtr: SaUtr, agentAuthDetails: AuthDetails, arn: Arn)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    for {
      delegatedAgentUserIds <- espAuthorisationService.getDelegatedAgentUserIdsFor(saUtr)
      (authorised, isAuthorisedInESP, maybeCesa) <- if (delegatedAgentUserIds.isEmpty)
        Future.successful(
          (notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$saUtr clientHasDelegatedAgents=false"), false, None))
      else authoriseMtdAgentForIRSA(delegatedAgentUserIds, agentCode, saUtr, agentAuthDetails, arn)
    } yield {
      auditDecision(
        agentCode,
        agentAuthDetails,
        "sa",
        saUtr,
        authorised,
        "cesaResult" -> desResultDescription(maybeCesa),
        "enrolmentStoreResult" -> isAuthorisedInESP)
      authorised
    }
  }

  //noinspection ScalaStyle
  def authoriseMtdAgentForIRSA(delegatedAgentUserIds: Set[AgentUserId], agentCode: AgentCode, saUtr: SaUtr, agentAuthDetails: AuthDetails, arn: Arn)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[(Boolean, Boolean, Option[Boolean])] = for {
    saAgentReferences <- mappingConnector.getAgentMappings("sa", arn).map(_.mappings.map(m => SaAgentReference(m.identifier)))
    agentUserIdsMap <- espAuthorisationService.getAgentUserIdsFor(saAgentReferences)
    (found, maybeCesa) <- Future.traverse(agentUserIdsMap) {
      case (saAgentReference, agentUserIds) =>
        val matchingAgentUserIds = delegatedAgentUserIds.intersect(agentUserIds)
        if (matchingAgentUserIds.isEmpty) {
          Logger.warn(s"Relationship not found in EACD for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} saAgentReference=${saAgentReference.value} client=${saUtr.value}")
          Future.successful((false, None))
        } else desAuthorisationService.isAuthorisedInCesa(agentCode, saAgentReference, saUtr)
          .andThen {
            case Success(false) =>
              Logger.warn(s"Relationship not found in CESA for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} saAgentReference=${saAgentReference.value} client=${saUtr.value}")
            case Failure(e) =>
              Logger.warn(s"Could not check relationship in CESA for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} saAgentReference=${saAgentReference.value} client=${saUtr.value}", e)
          }
          .map(b => (b, Some(b)))
          .recover {
            case NonFatal(_) => (false, Some(false))
          }
    }.map(results => results.collectFirst { case (true, c) => (true, c) }.getOrElse((false, results.collectFirst { case (_, Some(b)) => b })))
  } yield (
    if (found) {
      authorised(s"Access allowed for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} client=${saUtr.value}")
    } else {
      notAuthorised(s"Access not allowed for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} client=${saUtr.value} clientHasDelegatedAgents=true")
    }, true, maybeCesa)

  def isAuthorisedForPaye(agentCode: AgentCode, empRef: EmpRef)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    authConnector.currentAuthDetails().flatMap {
      case Some(agentAuthDetails @ AuthDetails(_, _, ggCredentialId, _, _)) =>
        for {
          isAuthorisedInESP <- espAuthorisationService.isAuthorisedForPayeInEnrolmentStoreProxy(ggCredentialId, empRef)
          maybeEbs <- if (isAuthorisedInESP) desAuthorisationService.isAuthorisedInEbs(agentCode, empRef).map(Some.apply)
          else Future successful None
        } yield {
          val result = isAuthorisedInESP && maybeEbs.get

          val ebsDescription = desResultDescription(maybeEbs)
          auditDecision(agentCode, agentAuthDetails, "paye", empRef, result, "ebsResult" -> ebsDescription, "enrolmentStoreResult" -> isAuthorisedInESP)

          if (result) authorised(s"Access allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$empRef")
          else notAuthorised(s"Access not allowed for agentCode=$agentCode ggCredential=${agentAuthDetails.ggCredentialId} client=$empRef esp=$isAuthorisedInESP ebs=$ebsDescription")
        }
      case None => Future successful notAuthorised("No user is logged in")
    }
  }

  private def desResultDescription(maybeEbs: Option[Boolean]): Any = {
    maybeEbs.getOrElse("notChecked")
  }

  def isAuthorisedForAfi(agentCode: AgentCode, nino: Nino)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Boolean] = {
    authConnector.currentAuthDetails().flatMap {
      case Some(authDetails @ AuthDetails(_, Some(arn), _, _, _)) =>
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
    agentCode: AgentCode,
    agentAuthDetails: AuthDetails,
    regime: String,
    taxIdentifier: TaxIdentifier,
    result: Boolean,
    extraDetails: (String, Any)*)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {

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
      Seq("credId" -> agentAuthDetails.ggCredentialId, "accessGranted" -> result)
        ++ extraDetails
        ++ optionalDetails)
  }
}
