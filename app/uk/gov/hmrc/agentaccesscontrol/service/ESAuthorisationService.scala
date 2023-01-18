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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlDecision,
  AuditService
}
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.model.AuthDetails
import uk.gov.hmrc.agentmtdidentifiers.model.{
  Arn,
  EnrolmentKey,
  TrustTaxIdentifier,
  Urn,
  Utr
}
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ESAuthorisationService @Inject()(
    relationshipsConnector: RelationshipsConnector,
    val desAgentClientApiConnector: DesAgentClientApiConnector,
    agentPermissionsConnector: AgentPermissionsConnector,
    auditService: AuditService,
    appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends LoggingAuthorisationResults
    with AgentSuspensionChecker {

  def authoriseForMtdVat(agentCode: AgentCode,
                         taxIdentifier: TaxIdentifier,
                         authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authoriseFor(agentCode, taxIdentifier, "HMRC-MTD-VAT", authDetails)

  def authoriseForMtdIt(agentCode: AgentCode,
                        taxIdentifier: TaxIdentifier,
                        authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authoriseFor(agentCode, taxIdentifier, "HMRC-MTD-IT", authDetails)

  def authoriseForTrust(agentCode: AgentCode,
                        trustTaxIdentifier: TrustTaxIdentifier,
                        authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] = {
    trustTaxIdentifier match {
      case Utr(v) =>
        authoriseFor(agentCode, Utr(v), "HMRC-TERS-ORG", authDetails)
      case Urn(v) =>
        authoriseFor(agentCode, Urn(v), "HMRC-TERSNT-ORG", authDetails)
      case e => throw new Exception(s"unhandled TrustTaxIdentifier $e")
    }
  }

  def authoriseForCgt(agentCode: AgentCode,
                      taxIdentifier: TaxIdentifier,
                      authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authoriseFor(agentCode, taxIdentifier, "HMRC-CGT-PD", authDetails)

  def authoriseForPpt(agentCode: AgentCode,
                      taxIdentifier: TaxIdentifier,
                      authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authoriseFor(agentCode, taxIdentifier, "HMRC-PPT-ORG", authDetails)

  private def authoriseFor(agentCode: AgentCode,
                           taxIdentifier: TaxIdentifier,
                           regime: String,
                           authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authDetails match {
      case agentAuthDetails @ AuthDetails(_, Some(arn), _, _, userRoleOpt) =>
        withSuspensionCheck(arn, getDesRegimeFor(regime)) {
          authoriseBasedOnRelationships(agentCode,
                                        taxIdentifier,
                                        regime,
                                        agentAuthDetails,
                                        arn,
                                        userRoleOpt)
        }

      case _ =>
        auditDecision(agentCode,
                      authDetails,
                      taxIdentifier,
                      result = false,
                      regime)
        Future.successful(notAuthorised(
          s"No ARN found in HMRC-AS-AGENT enrolment for agentCode $agentCode"))
    }

  private def authoriseBasedOnRelationships(
      agentCode: AgentCode,
      taxIdentifier: TaxIdentifier,
      regime: String,
      agentAuthDetails: AuthDetails,
      arn: Arn,
      userRoleOpt: Option[CredentialRole])(implicit hc: HeaderCarrier,
                                           request: Request[_]) = {
    checkForRelationship(arn,
                         Some(agentAuthDetails.ggCredentialId),
                         regime,
                         taxIdentifier)
      .map { result =>
        auditDecision(agentCode,
                      agentAuthDetails,
                      taxIdentifier,
                      result,
                      regime,
                      "arn" -> arn.value)
        if (result)
          authorised(
            s"Access allowed for agentCode=$agentCode arn=${arn.value} client=${taxIdentifier.value} userRole: ${userRoleOpt
              .getOrElse("None found")}")
        else
          notAuthorised(
            s"Access not allowed for agentCode=$agentCode arn=${arn.value} client=${taxIdentifier.value} userRole: ${userRoleOpt
              .getOrElse("None found")}")
      }
  }

  private def getDesRegimeFor(regime: String) = {
    regime match {
      case "HMRC-MTD-IT"     => "ITSA"
      case "HMRC-MTD-VAT"    => "VATC"
      case "HMRC-TERS-ORG"   => "TRS"
      case "HMRC-TERSNT-ORG" => "TRS" //this is the same with "HMRC-TERS-ORG"
      case "HMRC-CGT-PD"     => "CGT"
      case "HMRC-PPT-ORG"    => "PPT"
    }
  }

  private def auditDecision(agentCode: AgentCode,
                            agentAuthDetails: AuthDetails,
                            taxIdentifier: TaxIdentifier,
                            result: Boolean,
                            regime: String,
                            extraDetails: (String, Any)*)(
      implicit hc: HeaderCarrier,
      request: Request[Any],
      ec: ExecutionContext): Future[Unit] =
    auditService.auditEvent(
      AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      regime,
      taxIdentifier,
      Seq("credId" -> agentAuthDetails.ggCredentialId,
          "accessGranted" -> result) ++ extraDetails
    )

  def checkForRelationship(arn: Arn,
                           maybeUserId: Option[String],
                           regime: String,
                           taxIdentifier: TaxIdentifier)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[Boolean] = {

    for {
      // Check whether Granular Permissions are enabled and opted-in for this agent.
      // If so, when calling agent-client-relationships, we will check whether a relationship exists _for that specific agent user_.
      // (A relationship is also deemed to exist if the agent user is part of an appropriate tax service group)
      // If Granular Permissions are not enabled or opted out, we only check whether a relationship exists with the agency.
      granPermsEnabled <- if (!appConfig.enableGranularPermissions)
        Future.successful(false)
      else agentPermissionsConnector.granularPermissionsOptinRecordExists(arn)

      result <- (granPermsEnabled, maybeUserId) match {
        // Granular permissions are off: only check whether the agency as a whole has a relationship with the client
        case (false, _) =>
          relationshipsConnector.relationshipExists(arn, None, taxIdentifier)
        // If the user is part of a tax service group for the given tax service, allow access - no relationship needed
        // Otherwise check if the user has a relationship with the specific agent user
        case (true, Some(userId)) =>
          isAuthorisedBasedOnTaxServiceGroup(arn, userId, regime, taxIdentifier)
            .flatMap {
              case true => Future.successful(true)
              case false =>
                relationshipsConnector.relationshipExists(arn,
                                                          Some(userId),
                                                          taxIdentifier)
            }
        case (true, None) =>
          throw new IllegalArgumentException(
            "Cannot check for relationship: Granular Permissions are enabled but no user id is provided.")
      }
    } yield result
  }

  private def isAuthorisedBasedOnTaxServiceGroup(arn: Arn,
                                                 userId: String,
                                                 regime: String,
                                                 taxIdentifier: TaxIdentifier)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[Boolean] = {
    val taxGroupsServiceKey = regime match {
      case "HMRC-TERS-ORG" | "HMRC-TERSNT-ORG" =>
        "HMRC-TERS" // tax service groups use the nonstandard key "HMRC-TERS" to mean either type of trust
      case x => x
    }
    agentPermissionsConnector
      .getTaxServiceGroups(arn, taxGroupsServiceKey)
      .map {
        case Some(taxServiceAccessGroup) =>
          val isUserIdInTaxServiceGroup = taxServiceAccessGroup.teamMembers
            .getOrElse(Set.empty)
            .map(_.id)
            .contains(userId)
          val isClientExcluded = taxServiceAccessGroup.excludedClients
            .getOrElse(Set.empty)
            .exists(_.enrolmentKey == EnrolmentKey
              .enrolmentKey(serviceId = regime, clientId = taxIdentifier.value))
          isUserIdInTaxServiceGroup && !isClientExcluded
        case None => false
      }
  }

}
