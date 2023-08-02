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

import org.scalamock.scalatest.MockFactory
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.model.{AgentRecord, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.support.AuditSupport
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ESAuthorisationServiceSpec
    extends UnitSpec
    with MockFactory
    with AuditSupport {

  val relationshipsConnector: RelationshipsConnector =
    mock[RelationshipsConnector]
  val auditService: AuditService = mock[AuditService]
  val desAgentClientApiConnector: DesAgentClientApiConnector =
    mock[DesAgentClientApiConnector]
  val agentPermissionsConnector: AgentPermissionsConnector =
    stub[AgentPermissionsConnector]

  def appConfig: AppConfig = {
    val theStub = stub[ServicesConfig]
    (theStub
      .baseUrl(_: String))
      .when(*)
      .returns("blah-url")
    (theStub
      .getConfString(_: String, _: String))
      .when(*, *)
      .returns("blah-url")
    (theStub
      .getBoolean(_: String))
      .when("features.enable-granular-permissions")
      .returns(true)
    new AppConfig(theStub)
  }

  (agentPermissionsConnector
    .granularPermissionsOptinRecordExists(_: Arn)(_: HeaderCarrier,
                                                  _: ExecutionContext))
    .when(*, *, *)
    .returns(Future.successful(true))

  (agentPermissionsConnector
    .getTaxServiceGroups(_: Arn, _: String)(_: HeaderCarrier,
                                            _: ExecutionContext))
    .when(*, *, *, *)
    .returns(Future.successful(None))

  val esAuthService = new ESAuthorisationService(relationshipsConnector,
                                                 desAgentClientApiConnector,
                                                 agentPermissionsConnector,
                                                 auditService,
                                                 appConfig)

  val agentCode: AgentCode = AgentCode("agentCode")
  val arn: Arn = Arn("arn")
  val saAgentRef: SaAgentReference = SaAgentReference("ABC456")
  val clientId: MtdItId = MtdItId("clientId")
  val mtdAuthDetails: AuthDetails =
    AuthDetails(None, Some(arn), "ggId", Some("Agent"), Some(User))
  val nonMtdAuthDetails: AuthDetails =
    AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(User))
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent/arn/client/utr")
  val agentRecord: AgentRecord = AgentRecord(
    Some(SuspensionDetails(suspensionStatus = false, None)))

  def authoriseStandardBehaviours(service: Service,
                                  clientId: TaxIdentifier,
                                  regime: String): Unit = {
    "allow access for agent with a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning Future.successful(true)
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      givenAuditEvent()

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 nonMtdAuthDetails))

      result shouldBe false
    }

    "deny access for a mtd agent without a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning Future.successful(false)
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning Future.successful(true)
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))
      }

      "decision is made to deny access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning Future.successful(false)
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))
      }

      "no HMRC-AS-AGENT enrolment exists" in {
        givenAuditEvent()

        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 nonMtdAuthDetails))
      }
    }

    "handle suspended agents and return false" in {
      val agentRecord =
        AgentRecord(
          Some(SuspensionDetails(suspensionStatus = true, Some(Set(regime)))))

      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe false
    }

  }

  "authorise (MTD IT)" should {
    behave like authoriseStandardBehaviours(Service.MtdIt,
                                            MtdItId("clientId"),
                                            "ITSA")
  }
  "authorise (VAT)" should {
    behave like authoriseStandardBehaviours(Service.Vat, Vrn("vrn"), "ALL")
  }
  "authorise (Trust)" should {
    behave like authoriseStandardBehaviours(Service.Trust, Utr("utr"), "TRS")
  }
  "authorise (Trust NT)" should {
    behave like authoriseStandardBehaviours(Service.TrustNT, Urn("urn"), "TRS")
  }
  "authorise (CGT)" should {
    behave like authoriseStandardBehaviours(Service.CapitalGains,
                                            CgtRef("XMCGTP123456789"),
                                            "CGT")
  }
  "authorise (PPT)" should {
    behave like authoriseStandardBehaviours(Service.Ppt,
                                            PptRef("pptRef"),
                                            "AGSV")
  }

  "granular permissions logic" should {
    "when GP enabled and opted in, specify user to check in relationship service call" in {
      val cgtRef = CgtRef("XMCGTP123456789")
      givenAuditEvent()
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val agentPermissionsConnector = stub[AgentPermissionsConnector]
      (agentPermissionsConnector
        .granularPermissionsOptinRecordExists(_: Arn)(_: HeaderCarrier,
                                                      _: ExecutionContext))
        .when(*, *, *)
        .returns(Future.successful(true)) // user is opted-in to granular permissions
      (agentPermissionsConnector
        .getTaxServiceGroups(_: Arn, _: String)(_: HeaderCarrier,
                                                _: ExecutionContext))
        .when(*, *, *, *)
        .returns(Future.successful(None)) // this endpoint will be called so we must provide a stub

      val stubRelationshipsConnector = stub[RelationshipsConnector]
      (stubRelationshipsConnector
        .relationshipExists(_: Arn, _: Option[String], _: TaxIdentifier)(
          _: ExecutionContext,
          _: HeaderCarrier))
        .when(*, *, *, *, *)
        .returns(Future.successful(true))
      val esaService = new ESAuthorisationService(stubRelationshipsConnector,
                                                  desAgentClientApiConnector,
                                                  agentPermissionsConnector,
                                                  auditService,
                                                  appConfig)

      await(
        esaService.authoriseStandardService(agentCode,
                                            cgtRef,
                                            Service.CapitalGains.id,
                                            mtdAuthDetails))

      (stubRelationshipsConnector
        .relationshipExists(_: Arn, _: Option[String], _: TaxIdentifier)(
          _: ExecutionContext,
          _: HeaderCarrier))
        .verify(arn, Some("ggId"), cgtRef, *, *) // Check that we have specified the user id
    }

    "when GP outed out, do NOT specify user to check in relationship service call" in {
      val cgtRef = CgtRef("XMCGTP123456789")
      givenAuditEvent()
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val agentPermissionsConnector = stub[AgentPermissionsConnector]
      (agentPermissionsConnector
        .granularPermissionsOptinRecordExists(_: Arn)(_: HeaderCarrier,
                                                      _: ExecutionContext))
        .when(*, *, *)
        .returns(Future.successful(false)) // user is opted-out of granular permissions

      val stubRelationshipsConnector = stub[RelationshipsConnector]
      (stubRelationshipsConnector
        .relationshipExists(_: Arn, _: Option[String], _: TaxIdentifier)(
          _: ExecutionContext,
          _: HeaderCarrier))
        .when(*, *, *, *, *)
        .returns(Future.successful(true))
      val esaService = new ESAuthorisationService(stubRelationshipsConnector,
                                                  desAgentClientApiConnector,
                                                  agentPermissionsConnector,
                                                  auditService,
                                                  appConfig)

      await(
        esaService.authoriseStandardService(agentCode,
                                            cgtRef,
                                            Service.CapitalGains.id,
                                            mtdAuthDetails))

      (stubRelationshipsConnector
        .relationshipExists(_: Arn, _: Option[String], _: TaxIdentifier)(
          _: ExecutionContext,
          _: HeaderCarrier))
        .verify(arn, None, cgtRef, *, *) // Check that we have NOT specified the user id
    }
  }

  def whenRelationshipsConnectorIsCalled =
    (relationshipsConnector
      .relationshipExists(_: Arn, _: Option[String], _: MtdItId)(
        _: ExecutionContext,
        _: HeaderCarrier))
      .expects(*, *, *, *, *)
      .atLeastOnce()

  def whenDesAgentClientApiConnectorIsCalled =
    (desAgentClientApiConnector
      .getAgentRecord(_: TaxIdentifier)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
}
