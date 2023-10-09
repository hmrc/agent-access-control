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
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.{
  AgentClientAuthorisationConnector,
  RelationshipsConnector
}
import uk.gov.hmrc.agentaccesscontrol.model.{
  AccessResponse,
  AgentRecord,
  AuthDetails
}
import uk.gov.hmrc.agentaccesscontrol.support.AuditSupport
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ESAuthorisationServiceSpec extends UnitSpec with MockFactory {

  trait TestContext extends AuditSupport {
    lazy val auditService: AuditService = mock[AuditService]
    lazy val relationshipsConnector: RelationshipsConnector =
      mock[RelationshipsConnector]
    lazy val desAgentClientApiConnector: DesAgentClientApiConnector =
      mock[DesAgentClientApiConnector]
    lazy val mockAcaConnector: AgentClientAuthorisationConnector =
      mock[AgentClientAuthorisationConnector]
    lazy val agentPermissionsConnector: AgentPermissionsConnector =
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
                                                   mockAcaConnector,
                                                   agentPermissionsConnector,
                                                   auditService,
                                                   appConfig)

    def whenAgencyHasARelationshipWithClient =
      (relationshipsConnector
        .relationshipExists(_: Arn, _: Option[String], _: MtdItId)(
          _: ExecutionContext,
          _: HeaderCarrier))
        .expects(*, None, *, *, *)
        .atLeastOnce()

    def whenAgencyHasARelationshipWithClientAndAgentUserIsAssigned(
        userId: String) =
      (relationshipsConnector
        .relationshipExists(_: Arn, _: Option[String], _: MtdItId)(
          _: ExecutionContext,
          _: HeaderCarrier))
        .expects(*, Some(userId), *, *, *)
        .atLeastOnce()

    def whenAcaReturnsAgentNotSuspended() =
      (mockAcaConnector
        .getSuspensionDetails(_: Arn)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(SuspensionDetails.notSuspended))
  }

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
    "allow access for agent with a client relationship" in new TestContext {
      givenAuditEvent()
      whenAgencyHasARelationshipWithClient returning Future.successful(true)
      whenAgencyHasARelationshipWithClientAndAgentUserIsAssigned(
        mtdAuthDetails.ggCredentialId) returning Future.successful(true)
      whenAcaReturnsAgentNotSuspended()

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe AccessResponse.Authorised
    }

    "deny access for a non-mtd agent" in new TestContext {
      givenAuditEvent()

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 nonMtdAuthDetails))

      result shouldBe AccessResponse.NoRelationship
    }

    "deny access for a mtd agent without a client relationship" in new TestContext {
      givenAuditEvent()
      whenAgencyHasARelationshipWithClient returning Future.successful(false)
      whenAcaReturnsAgentNotSuspended()

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe AccessResponse.NoRelationship
    }

    "deny access for a mtd agent with a client relationship but agent user is unassigned (and access groups turned on)" in new TestContext {
      givenAuditEvent()
      whenAgencyHasARelationshipWithClient returning Future.successful(true)
      whenAgencyHasARelationshipWithClientAndAgentUserIsAssigned(
        mtdAuthDetails.ggCredentialId) returning Future.successful(false)
      whenAcaReturnsAgentNotSuspended()
      (agentPermissionsConnector
        .granularPermissionsOptinRecordExists(_: Arn)(_: HeaderCarrier,
                                                      _: ExecutionContext))
        .when(*, *, *)
        .returns(Future.successful(true))
        .anyNumberOfTimes() // user is opted-in to granular permissions

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe AccessResponse.NoAssignment
    }

    "audit appropriate values" when {
      "decision is made to allow access" in new TestContext {
        givenAuditEvent()
        whenAgencyHasARelationshipWithClient returning Future.successful(true)
        whenAcaReturnsAgentNotSuspended()

        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))
      }

      "decision is made to deny access" in new TestContext {
        givenAuditEvent()
        whenAgencyHasARelationshipWithClient returning Future.successful(false)
        whenAcaReturnsAgentNotSuspended()

        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))
      }

      "no HMRC-AS-AGENT enrolment exists" in new TestContext {
        givenAuditEvent()

        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 nonMtdAuthDetails))
      }
    }

    "handle suspended agents" in new TestContext {
      (mockAcaConnector
        .getSuspensionDetails(_: Arn)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(
          SuspensionDetails(suspensionStatus = true, Some(Set(regime)))))

      val result =
        await(
          esAuthService.authoriseStandardService(agentCode,
                                                 clientId,
                                                 service.id,
                                                 mtdAuthDetails))

      result shouldBe AccessResponse.AgentSuspended
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
    "when GP enabled and opted in, specify user to check in relationship service call" in new TestContext {
      val cgtRef = CgtRef("XMCGTP123456789")
      givenAuditEvent()
      whenAcaReturnsAgentNotSuspended()

      override lazy val agentPermissionsConnector =
        stub[AgentPermissionsConnector]
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
      val esaService =
        new ESAuthorisationService(stubRelationshipsConnector,
                                   desAgentClientApiConnector,
                                   mockAcaConnector,
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

    "when GP outed out, do NOT specify user to check in relationship service call" in new TestContext {
      val cgtRef = CgtRef("XMCGTP123456789")
      givenAuditEvent()
      whenAcaReturnsAgentNotSuspended()

      override lazy val agentPermissionsConnector =
        stub[AgentPermissionsConnector]
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
      val esaService =
        new ESAuthorisationService(stubRelationshipsConnector,
                                   desAgentClientApiConnector,
                                   mockAcaConnector,
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

}
