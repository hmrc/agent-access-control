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

package uk.gov.hmrc.agentaccesscontrol.service

import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlDecision,
  AuditService
}
import uk.gov.hmrc.agentaccesscontrol.connectors._
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentaccesscontrol.model.AuthDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
class AuthorisationService @Inject()(
    desAuthorisationService: DesAuthorisationService,
    espAuthorisationService: EnrolmentStoreProxyAuthorisationService,
    auditService: AuditService,
    mappingConnector: MappingConnector,
    afiRelationshipConnector: AfiRelationshipConnector,
    val agentClientAuthorisationConnector: AgentClientAuthorisationConnector)
    extends LoggingAuthorisationResults
    with AgentSuspensionChecker {

  private val accessGranted = true
  private val accessDenied = false

  def isAuthorisedForSa(agentCode: AgentCode,
                        saUtr: SaUtr,
                        authDetails: AuthDetails)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      request: Request[Any]): Future[Boolean] =
    authDetails match {
      case nonMtdAgentAuthDetails @ AuthDetails(Some(saAgentReference),
                                                _,
                                                _,
                                                _,
                                                _) =>
        authoriseNonMtdAgentForIRSA(agentCode,
                                    saUtr,
                                    nonMtdAgentAuthDetails,
                                    saAgentReference)
      case mtdAgentAuthDetails @ AuthDetails(_, Some(arn), _, _, _) =>
        authoriseMtdAgentForIRSA(agentCode, saUtr, mtdAgentAuthDetails, arn)
      case agentAuthDetails @ AuthDetails(None, _, _, _, _) =>
        auditDecision(agentCode, agentAuthDetails, "sa", saUtr, result = false)
        Future successful notAuthorised(
          s"No 6 digit agent reference found for agent $agentCode")
      case _ =>
        Future successful notAuthorised(
          "agent did not have valid set of auth details to proceed")
    }

  //noinspection ScalaStyle
  def authoriseNonMtdAgentForIRSA(agentCode: AgentCode,
                                  saUtr: SaUtr,
                                  agentAuthDetails: AuthDetails,
                                  saAgentReference: SaAgentReference)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      request: Request[Any]): Future[Boolean] =
    for {
      isAuthorisedInESP <- espAuthorisationService
        .isAuthorisedForSaInEnrolmentStoreProxy(agentAuthDetails.ggCredentialId,
                                                saUtr)
      maybeCesa <- if (isAuthorisedInESP)
        desAuthorisationService
          .isAuthorisedInCesa(agentCode, saAgentReference, saUtr)
          .map(Some.apply)
      else Future successful None
    } yield {
      val result = isAuthorisedInESP && maybeCesa.get

      val cesaDescription = desResultDescription(maybeCesa)
      auditDecision(agentCode,
                    agentAuthDetails,
                    "sa",
                    saUtr,
                    result,
                    "cesaResult" -> cesaDescription,
                    "enrolmentStoreResult" -> isAuthorisedInESP)

      if (result)
        authorised(agentCode,
                   saUtr,
                   agentAuthDetails.ggCredentialId,
                   Some(saAgentReference))
      else
        notAuthorised(agentCode,
                      saUtr,
                      agentAuthDetails.ggCredentialId,
                      Some(saAgentReference))
    }

  //noinspection ScalaStyle
  def authoriseMtdAgentForIRSA(agentCode: AgentCode,
                               saUtr: SaUtr,
                               agentAuthDetails: AuthDetails,
                               arn: Arn)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      request: Request[Any]): Future[Boolean] =
    for {
      delegatedAgentUserIds <- espAuthorisationService
        .getDelegatedAgentUserIdsFor(saUtr)
      (authorised, isAuthorisedInESP, maybeCesa) <- if (delegatedAgentUserIds.isEmpty)
        Future.successful(
          (notAuthorised(agentCode,
                         saUtr,
                         agentAuthDetails.ggCredentialId,
                         Some(arn),
                         Some(false)),
           false,
           None))
      else
        authoriseMtdAgentForIRSA(delegatedAgentUserIds,
                                 agentCode,
                                 saUtr,
                                 agentAuthDetails,
                                 arn)
    } yield {
      auditDecision(agentCode,
                    agentAuthDetails,
                    "sa",
                    saUtr,
                    authorised,
                    "cesaResult" -> desResultDescription(maybeCesa),
                    "enrolmentStoreResult" -> isAuthorisedInESP)
      authorised
    }

  //noinspection ScalaStyle
  def authoriseMtdAgentForIRSA(delegatedAgentUserIds: Set[AgentUserId],
                               agentCode: AgentCode,
                               saUtr: SaUtr,
                               agentAuthDetails: AuthDetails,
                               arn: Arn)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[(Boolean, Boolean, Option[Boolean])] =
    for {
      saAgentReferences <- mappingConnector
        .getAgentMappings("sa", arn)
        .map(_.mappings.map(m => SaAgentReference(m.identifier)))
      agentUserIdsMap <- espAuthorisationService.getAgentUserIdsFor(
        saAgentReferences)
      (found, maybeCesa) <- Future
        .traverse(agentUserIdsMap) {
          case (saAgentReference, agentUserIds) =>
            val matchingAgentUserIds =
              delegatedAgentUserIds.intersect(agentUserIds)
            if (matchingAgentUserIds.isEmpty) {
              logger.warn(
                s"Relationship not found in EACD for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} saAgentReference=${saAgentReference.value} client=${saUtr.value}")
              Future.successful((false, None))
            } else
              desAuthorisationService
                .isAuthorisedInCesa(agentCode, saAgentReference, saUtr)
                .andThen {
                  case Success(false) =>
                    logger.info(
                      s"Relationship not found in CESA for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} saAgentReference=${saAgentReference.value} client=${saUtr.value}")
                  case Failure(e) =>
                    logger.info(
                      s"Could not check relationship in CESA for arn=${arn.value} agentCode=${agentCode.value} agentUserId=${agentAuthDetails.ggCredentialId} saAgentReference=${saAgentReference.value} client=${saUtr.value}",
                      e
                    )
                }
                .map(b => (b, Some(b)))
                .recover {
                  case NonFatal(_) => (false, Some(false))
                }
        }
        .map(results =>
          results
            .collectFirst { case (true, c) => (true, c) }
            .getOrElse(
              (false, results.collectFirst { case (_, Some(b)) => b })))
    } yield
      (if (found) {
        authorised(agentCode, saUtr, agentAuthDetails.ggCredentialId, Some(arn))
      } else {
        notAuthorised(agentCode,
                      saUtr,
                      agentAuthDetails.ggCredentialId,
                      Some(arn),
                      Some(true))
      }, true, maybeCesa)

  def isAuthorisedForPaye(agentCode: AgentCode,
                          empRef: EmpRef,
                          authDetails: AuthDetails)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      request: Request[Any]): Future[Boolean] =
    authDetails match {
      case agentAuthDetails @ AuthDetails(_, _, ggCredentialId, _, _) =>
        for {
          isAuthorisedInESP <- espAuthorisationService
            .isAuthorisedForPayeInEnrolmentStoreProxy(ggCredentialId, empRef)
          maybeEbs <- if (isAuthorisedInESP)
            desAuthorisationService
              .isAuthorisedInEbs(agentCode, empRef)
              .map(Some.apply)
          else Future successful None
        } yield {
          val result = isAuthorisedInESP && maybeEbs.get

          val ebsDescription = desResultDescription(maybeEbs)
          auditDecision(agentCode,
                        agentAuthDetails,
                        "paye",
                        empRef,
                        result,
                        "ebsResult" -> ebsDescription,
                        "enrolmentStoreResult" -> isAuthorisedInESP)

          if (result)
            authorised(agentCode, empRef, agentAuthDetails.ggCredentialId)
          else notAuthorised(agentCode, empRef, agentAuthDetails.ggCredentialId)
        }
      case _ => Future successful notAuthorised("No user is logged in")
    }

  private def desResultDescription(maybeEbs: Option[Boolean]): Any =
    maybeEbs.getOrElse("notChecked")

  def isAuthorisedForAfi(agentCode: AgentCode,
                         nino: Nino,
                         authDetails: AuthDetails)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      request: Request[Any]): Future[Boolean] =
    authDetails match {
      case authDetails @ AuthDetails(_, Some(arn), _, _, _) =>
        withSuspensionCheck(arn, "PIR") {
          authoriseBasedOnAfiRelationships(agentCode, nino, authDetails, arn)
        }
      case _ => Future successful notFound("Error retrieving arn")
    }

  private def authoriseBasedOnAfiRelationships(agentCode: AgentCode,
                                               nino: Nino,
                                               authDetails: AuthDetails,
                                               arn: Arn)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier,
      request: Request[Any]): Future[Boolean] = {
    afiRelationshipConnector.hasRelationship(arn.value, nino.value) map {
      hasRelationship =>
        if (hasRelationship) {
          auditDecision(agentCode,
                        authDetails,
                        "afi",
                        nino,
                        accessGranted,
                        "" -> "")
          found("Relationship Found")
        } else {
          auditDecision(agentCode,
                        authDetails,
                        "afi",
                        nino,
                        accessDenied,
                        "" -> "")
          notFound("No relationship found")
        }
    }
  }

  private def auditDecision(agentCode: AgentCode,
                            agentAuthDetails: AuthDetails,
                            regime: String,
                            taxIdentifier: TaxIdentifier,
                            result: Boolean,
                            extraDetails: (String, Any)*)(
      implicit hc: HeaderCarrier,
      request: Request[Any],
      ec: ExecutionContext): Future[Unit] = {

    val optionalDetails = Seq(
      agentAuthDetails.saAgentReference.map("saAgentReference" -> _),
      agentAuthDetails.affinityGroup.map("affinityGroup" -> _),
      agentAuthDetails.agentUserRole.map("agentUserRole" -> _)
    ).flatten

    auditService.auditEvent(
      AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      regime,
      taxIdentifier,
      Seq("credId" -> agentAuthDetails.ggCredentialId,
          "accessGranted" -> result)
        ++ extraDetails
        ++ optionalDetails
    )
  }
}
