/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlEvent,
  AuditService
}
import uk.gov.hmrc.agentaccesscontrol.connectors.AuthDetails
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ESAuthorisationService @Inject()(
    relationshipsConnector: RelationshipsConnector,
    desAgentClientApiConnector: DesAgentClientApiConnector,
    auditService: AuditService)(implicit ec: ExecutionContext)
    extends LoggingAuthorisationResults {

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
                        taxIdentifier: TaxIdentifier,
                        authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authoriseFor(agentCode, taxIdentifier, "HMRC-TERS-ORG", authDetails)

  def authoriseForCgt(agentCode: AgentCode,
                      taxIdentifier: TaxIdentifier,
                      authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authoriseFor(agentCode, taxIdentifier, "HMRC-CGT-PD", authDetails)

  private def authoriseFor(agentCode: AgentCode,
                           taxIdentifier: TaxIdentifier,
                           regime: String,
                           authDetails: AuthDetails)(
      implicit hc: HeaderCarrier,
      request: Request[_]): Future[Boolean] =
    authDetails match {
      case agentAuthDetails @ AuthDetails(_, Some(arn), _, _, userRoleOpt) =>
        withSuspensionCheck(
          arn,
          getDesRegimeFor(regime),
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
        )

      case _ =>
        auditDecision(agentCode,
                      authDetails,
                      taxIdentifier,
                      result = false,
                      regime)
        Future.successful(notAuthorised(
          s"No ARN found in HMRC-AS-AGENT enrolment for agentCode $agentCode"))
    }

  private def getDesRegimeFor(regime: String) = {
    regime match {
      case "HMRC-MTD-IT"   => "ITSA"
      case "HMRC-MTD-VAT"  => "VATC"
      case "HMRC-TERS-ORG" => "TRS"
      case "HMRC-CGT-PD"   => "CGT"
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
      AgentAccessControlEvent.AgentAccessControlDecision,
      "agent access decision",
      agentCode,
      regime,
      taxIdentifier,
      Seq("credId" -> agentAuthDetails.ggCredentialId,
          "accessGranted" -> result) ++ extraDetails
    )

  private def withSuspensionCheck(agentId: TaxIdentifier,
                                  regime: String,
                                  proceed: => Future[Boolean])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext) = {
    desAgentClientApiConnector.getAgentRecord(agentId).flatMap { agentRecord =>
      if (agentRecord.isSuspended) {
        if (agentRecord.suspendedFor(regime)) {
          Logger.warn(
            s"agent with id : ${agentId.value} is suspended for regime $regime")
          Future(false)
        } else {
          proceed
        }
      } else {
        proceed
      }
    }
  }
}
