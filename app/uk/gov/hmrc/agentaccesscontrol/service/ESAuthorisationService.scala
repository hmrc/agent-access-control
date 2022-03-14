/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.model.AuthDetails
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, TrustTaxIdentifier, Urn, Utr}
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ESAuthorisationService @Inject()(
    relationshipsConnector: RelationshipsConnector,
    val desAgentClientApiConnector: DesAgentClientApiConnector,
    auditService: AuditService)(implicit ec: ExecutionContext,
                                appConfig: AppConfig)
    extends LoggingAuthorisationResults
    with AgentSuspensionChecker {

  private lazy val isSuspensionEnabled = appConfig.featureAgentSuspension

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
        withSuspensionCheck(isSuspensionEnabled, arn, getDesRegimeFor(regime)) {
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
    relationshipsConnector.relationshipExists(arn, taxIdentifier).map {
      result =>
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

}
