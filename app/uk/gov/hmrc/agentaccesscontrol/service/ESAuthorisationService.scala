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

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlDecision,
  AuditService
}
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.{
  AgentClientAuthorisationConnector,
  RelationshipsConnector
}
import uk.gov.hmrc.agentaccesscontrol.model.{AccessResponse, AuthDetails}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ESAuthorisationService @Inject()(
    relationshipsConnector: RelationshipsConnector,
    val desAgentClientApiConnector: DesAgentClientApiConnector,
    val agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
    agentPermissionsConnector: AgentPermissionsConnector,
    auditService: AuditService,
    appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends AgentSuspensionChecker
    with Logging {

  def authoriseStandardService(agentCode: AgentCode,
                               taxIdentifier: TaxIdentifier,
                               serviceId: String,
                               authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[AccessResponse] =
    authDetails match {
      case agentAuthDetails @ AuthDetails(_, Some(arn), _, _, userRoleOpt) =>
        // TODO confirm with stakeholders if we can remove regime for suspension check?
        withSuspensionCheck(arn, getDesRegimeFor(serviceId)) {
          authoriseBasedOnRelationships(agentCode,
                                        taxIdentifier,
                                        serviceId,
                                        agentAuthDetails,
                                        arn,
                                        userRoleOpt)
        }

      case _ =>
        auditDecision(agentCode,
                      authDetails,
                      taxIdentifier,
                      result = false,
                      serviceId)
        logger.info(
          s"Not authorised: No ARN found in HMRC-AS-AGENT enrolment for agentCode $agentCode")
        Future.successful(AccessResponse.NoRelationship)
    }

  private def authoriseBasedOnRelationships(
      agentCode: AgentCode,
      taxIdentifier: TaxIdentifier,
      regime: String,
      agentAuthDetails: AuthDetails,
      arn: Arn,
      userRoleOpt: Option[CredentialRole])(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[AccessResponse] = {
    checkForRelationship(arn,
                         Some(agentAuthDetails.ggCredentialId),
                         regime,
                         taxIdentifier)
      .map { result =>
        auditDecision(
          agentCode,
          agentAuthDetails,
          taxIdentifier,
          result == AccessResponse.Authorised, // TODO should we audit the specific type of non authorised (no relationship/no assignment) or only true/false?
          regime,
          "arn" -> arn.value
        )
        result match {
          case AccessResponse.Authorised =>
            logger.info(
              s"Access allowed for agentCode=$agentCode arn=${arn.value} client=${taxIdentifier.value} userRole: ${userRoleOpt
                .getOrElse("None found")}")
          case otherResponse =>
            logger.info(
              s"Not authorised: Access not allowed for agentCode=$agentCode arn=${arn.value} client=${taxIdentifier.value} userRole: ${userRoleOpt
                .getOrElse("None found")}. Response: $otherResponse")
        }
        result
      }
  }

  private def getDesRegimeFor(regime: String) = {
    regime match {
      case "HMRC-MTD-IT"                         => "ITSA"
      case "HMRC-MTD-VAT"                        => "VATC"
      case "HMRC-TERS-ORG" | "HMRC-TERSNT-ORG"   => "TRS"
      case "HMRC-CGT-PD"                         => "CGT"
      case "HMRC-PPT-ORG"                        => "PPT"
      case "HMRC-CBC-ORG" | "HMRC-CBC-NONUK-ORG" => "CBC"
    }
  }

  //noinspection ScalaStyle
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
      hc: HeaderCarrier): Future[AccessResponse] = {

    for {
      // Does a relationship exist between agency (ARN) and client?
      agencyHasRelationship <- relationshipsConnector.relationshipExists(
        arn,
        None,
        taxIdentifier)
      // Check whether Granular Permissions are enabled and opted-in for this agent.
      // If so, when calling agent-client-relationships, we will check whether a relationship exists _for that specific agent user_.
      // (A relationship is also deemed to exist if the agent user is part of an appropriate tax service group)
      // If Granular Permissions are not enabled or opted out, we only check whether a relationship exists with the agency.
      granPermsEnabled <- if (!appConfig.enableGranularPermissions)
        Future.successful(false)
      else agentPermissionsConnector.granularPermissionsOptinRecordExists(arn)

      result <- (agencyHasRelationship, granPermsEnabled, maybeUserId) match {
        // The agency hasn't got a relationship at all with the client: deny access
        case (false, _, _) => Future.successful(AccessResponse.NoRelationship)
        // The agency has a relationship and granular permissions are off: grant access
        case (true, false, _) => Future.successful(AccessResponse.Authorised)
        // Granular permissions are enabled. Grant access if either:
        //  - the user is part of the tax service group for the given tax service, (no user relationship needed), or
        //  - the user has a relationship with the specific agent user
        case (true, true, Some(userId)) =>
          isAuthorisedBasedOnTaxServiceGroup(arn, userId, regime, taxIdentifier)
            .flatMap {
              case true => Future.successful(AccessResponse.Authorised)
              case false =>
                relationshipsConnector
                  .relationshipExists(arn, Some(userId), taxIdentifier)
                  .map {
                    case true  => AccessResponse.Authorised
                    case false => AccessResponse.NoAssignment
                  }
            }
        case (true, true, None) =>
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
      // These tax service groups use a truncated key to indicate either type
      case "HMRC-TERS-ORG" | "HMRC-TERSNT-ORG"   => "HMRC-TERS"
      case "HMRC-CBC-ORG" | "HMRC-CBC-NONUK-ORG" => "HMRC-CBC"
      case x                                     => x
    }
    agentPermissionsConnector
      .getTaxServiceGroups(arn, taxGroupsServiceKey)
      .map {
        case Some(taxGroup) =>
          val isUserIdInTaxServiceGroup = taxGroup.teamMembers
            .map(_.id)
            .contains(userId)
          val isClientExcluded = taxGroup.excludedClients
            .exists(_.enrolmentKey.contains(taxIdentifier.value))
          isUserIdInTaxServiceGroup && !isClientExcluded
        case None => false
      }
  }

}
