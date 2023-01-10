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

package uk.gov.hmrc.agentaccesscontrol.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, ControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.model.AuthDetails
import uk.gov.hmrc.agentaccesscontrol.service.{
  AuthorisationService,
  ESAuthorisationService
}
import uk.gov.hmrc.agentmtdidentifiers.model.{
  Arn,
  MtdItId,
  PptRef,
  TrustTaxIdentifier,
  Urn,
  Utr,
  Vrn
}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{Nino => _, _}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerSpec
    extends UnitSpec
    with BeforeAndAfterEach
    with MockFactory {

  val authorisationService = mock[AuthorisationService]
  val esAuthorisationService = mock[ESAuthorisationService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val cc: ControllerComponents = stubControllerComponents()
  val environment = mock[Environment]
  val arn = Arn("arn")
  val agentCode = "ABCDEF123456"
  val credentialRole = User
  val providerId = "12345-credId"

  def controller(appConfig: AppConfig = new AppConfig(mockServiceConfig())) = {
    new AuthorisationController(authorisationService,
                                mockAuthConnector,
                                esAuthorisationService,
                                environment,
                                cc)(global, appConfig)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  val agentEnrolment = Set(
    Enrolment("HMRC-AS-AGENT",
              Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
              state = "Active",
              delegatedAuthRule = None))

  val ggCredentials = Credentials("ggId", "GovernmentGateway")

  val authResponseMtdAgent
    : Future[~[~[~[Option[String], Enrolments], Option[CredentialRole]],
               Option[Credentials]]] =
    Future successful new ~(
      new ~(new ~(Some(agentCode), Enrolments(agentEnrolment)),
            Some(credentialRole)),
      Some(ggCredentials))

  val saAgentReference = SaAgentReference("enrol-123")

  val nonMtdAuthDetails = AuthDetails(saAgentReference = Some(saAgentReference),
                                      arn = None,
                                      ggCredentialId = "12345-credId",
                                      affinityGroup = Some("Agent"),
                                      agentUserRole = Some(credentialRole))

  val mtdAuthDetails = AuthDetails(saAgentReference = None,
                                   arn = Some(arn),
                                   "ggId",
                                   Some("Agent"),
                                   Some(User))

  private def anSaEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {

    "return 401 if the AuthorisationService doesn't permit access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future successful false)

      val response =
        controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the AuthorisationService allows access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future successful true)

      val response =
        controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    "pass request to AuthorisationService" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future successful true)

      val response =
        await(
          controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(
            fakeRequest))
      status(response) shouldBe Status.OK
    }
    "propagate exception if the AuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future failed new IllegalStateException("some error"))
      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForSa(AgentCode(agentCode),
                                              SaUtr("utr"))(fakeRequest)))
    }
  }

  private def anMdtItEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 401 if the MtdAuthorisationService doesn't permit access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdItAuthorisationServiceIsCalled.returning(Future successful false)

      val response =
        controller().isAuthorisedForMtdIt(AgentCode(agentCode),
                                          MtdItId("mtdItId"))(fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the MtdAuthorisationService allows access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdItAuthorisationServiceIsCalled.returning(Future successful true)

      val response =
        controller().isAuthorisedForMtdIt(AgentCode(agentCode),
                                          MtdItId("mtdItId"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "propagate exception if the MtdAuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdItAuthorisationServiceIsCalled.returning(
        Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(
          controller().isAuthorisedForMtdIt(AgentCode(agentCode),
                                            MtdItId("mtdItId"))(fakeRequest)))
    }
  }

  private def anMtdVatEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 401 if the MtdVatAuthorisationService doesn't permit access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdVatAuthorisationServiceIsCalled.returning(Future successful false)

      val response = controller().isAuthorisedForMtdVat(AgentCode(agentCode),
                                                        Vrn("vrn"))(fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the MtdVatAuthorisationService allows access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdVatAuthorisationServiceIsCalled.returning(Future successful true)

      val response = controller().isAuthorisedForMtdVat(AgentCode(agentCode),
                                                        Vrn("vrn"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "propagate exception if the MtdVatAuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdVatAuthorisationServiceIsCalled.returning(
        Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForMtdVat(AgentCode(agentCode),
                                                  Vrn("vrn"))(fakeRequest)))
    }
  }

  private def aPptEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 401 if the MtdPptAuthorisationService doesn't permit access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPptAuthorisationServiceIsCalled.returning(Future successful false)

      val response =
        controller().isAuthorisedForPpt(AgentCode(agentCode), PptRef("pptRef"))(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the MtdPptAuthorisationService allows access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPptAuthorisationServiceIsCalled.returning(Future successful true)

      val response =
        controller().isAuthorisedForPpt(AgentCode(agentCode), PptRef("pptRef"))(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    "propagate exception if the MtdPptAuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPptAuthorisationServiceIsCalled.returning(
        Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForPpt(AgentCode(agentCode),
                                               PptRef("pptRef"))(fakeRequest)))
    }
  }

  private def aPayeEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 200 when Paye is enabled" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPayeAuthorisationServiceIsCalled.returning(Future successful true)

      val response =
        controller().isAuthorisedForPaye(AgentCode(agentCode),
                                         EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 200
    }

    "return 403 when Paye is disabled" in {
      whenAuthIsCalled(authResponseMtdAgent)
      val response =
        controller(new AppConfig(mockServiceConfig(false))).isAuthorisedForPaye(
          AgentCode(agentCode),
          EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 403
    }
  }

  private def anAfiEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {

    "return 200 if the AuthorisationService allows access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled.returning(Future successful true)

      val response =
        controller().isAuthorisedForAfi(AgentCode(agentCode),
                                        Nino("AA123456A"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return 401 if the AuthorisationService does not allow access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled.returning(Future successful false)

      val response =
        controller().isAuthorisedForAfi(AgentCode(agentCode),
                                        Nino("AA123456A"))(fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "propagate exception if the AuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled.returning(
        Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForAfi(AgentCode(agentCode),
                                               Nino("AA123456A"))(fakeRequest)))
    }

  }

  private def aTrustEndpoint(
      fakeRequest: FakeRequest[_ <: AnyContent]): Unit = {

    "return 200 when the point is called with Utr" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenTrustAuthorisationServiceIsCalled.returning(Future successful true)
      val response =
        controller().isAuthorisedForTrust(AgentCode(agentCode),
                                          Utr("0123456789"))(fakeRequest)
      status(response) shouldBe 200
    }

    "return 200 when the point is called with Urn" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenTrustAuthorisationServiceIsCalled.returning(Future successful true)
      val response =
        controller().isAuthorisedForTrust(AgentCode(agentCode),
                                          Urn("urn123456"))(fakeRequest)
      status(response) shouldBe 200
    }
  }

  "GET isAuthorisedForSa" should {
    behave like anSaEndpoint(
      FakeRequest("GET", "/agent-access-control/sa-auth/agent//client/utr"))
  }

  "POST isAuthorisedForSa" should {
    behave like anSaEndpoint(
      FakeRequest("POST", "/agent-access-control/sa-auth/agent/client/utr")
        .withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForMtdIt" should {
    behave like anMdtItEndpoint(
      FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent//client/utr"))
  }

  "POST isAuthorisedForMtdIt" should {
    behave like anMdtItEndpoint(
      FakeRequest("POST", "/agent-access-control/mtd-it-auth/agent//client/utr")
        .withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForMtdVat" should {
    behave like anMtdVatEndpoint(
      FakeRequest("GET",
                  "/agent-access-control/mtd-vat-auth/agent//client/utr"))
  }

  "POST isAuthorisedForMtdVat" should {
    behave like anMtdVatEndpoint(
      FakeRequest("POST",
                  "/agent-access-control/mtd-vat-auth/agent//client/utr")
        .withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForPaye" should {
    behave like aPayeEndpoint(
      FakeRequest("GET", "/agent-access-control/epaye-auth/agent//client/utr"))
  }

  "POST isAuthorisedForPaye" should {
    behave like aPayeEndpoint(
      FakeRequest("POST", "/agent-access-control/epaye-auth/agent//client/utr")
        .withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForAfi" should {
    behave like anAfiEndpoint(
      FakeRequest("GET", "/agent-access-control/afi-auth/agent//client/utr"))
  }

  "POST isAuthorisedForAfi" should {
    behave like anAfiEndpoint(
      FakeRequest("POST", "/agent-access-control/afi-auth/agent//client/utr")
        .withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForNonTaxableTrust" should {
    behave like aTrustEndpoint(
      FakeRequest(
        "GET",
        "/agent-access-control/non-taxable-trust-auth/agent//client/urn"))
  }

  "POST isAuthorisedForNonTaxableTrust" should {
    behave like aTrustEndpoint(
      FakeRequest(
        "POST",
        "/agent-access-control/non-taxable-trust-auth/agent//client/urn")
        .withJsonBody(Json.parse("{}")))
  }

  "POST isAuthorisedForPpt" should {
    behave like aPptEndpoint(
      FakeRequest("POST", "/agent-access-control/ppt-auth/agent//client/utr")
        .withJsonBody(Json.parse("{}")))
  }

  def whenAuthIsCalled(
      returnValue: Future[
        ~[~[~[Option[String], Enrolments], Option[CredentialRole]],
          Option[Credentials]]]) =
    (
      mockAuthConnector
        .authorise(_: Predicate,
                   _: Retrieval[~[~[~[Option[String], Enrolments],
                                    Option[CredentialRole]],
                                  Option[Credentials]]])(
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *)
      .returning(returnValue)

  def whenAfiAuthorisationServiceIsCalled =
    (authorisationService
      .isAuthorisedForAfi(_: AgentCode, _: Nino, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *, *)

  def whenAuthorisationServiceIsCalled(authDetails: AuthDetails) =
    (authorisationService
      .isAuthorisedForSa(_: AgentCode, _: SaUtr, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *, *)

  def whenMtdItAuthorisationServiceIsCalled =
    (esAuthorisationService
      .authoriseForMtdIt(_: AgentCode, _: MtdItId, _: AuthDetails)(
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *)

  def whenMtdVatAuthorisationServiceIsCalled =
    (esAuthorisationService
      .authoriseForMtdVat(_: AgentCode, _: Vrn, _: AuthDetails)(
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *)

  def whenPptAuthorisationServiceIsCalled =
    (esAuthorisationService
      .authoriseForPpt(_: AgentCode, _: PptRef, _: AuthDetails)(
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *)

  def whenPayeAuthorisationServiceIsCalled =
    (authorisationService
      .isAuthorisedForPaye(_: AgentCode, _: EmpRef, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *, *)

  def whenTrustAuthorisationServiceIsCalled =
    (esAuthorisationService
      .authoriseForTrust(_: AgentCode, _: TrustTaxIdentifier, _: AuthDetails)(
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *)

  private def mockServiceConfig(allowPayeAccess: Boolean = true) = {
    val servicesConfig = mock[ServicesConfig]
    (servicesConfig
      .baseUrl(_: String))
      .expects(*)
      .atLeastOnce()
      .returning("blah-url")
    (servicesConfig
      .getConfString(_: String, _: String))
      .expects(*, *)
      .atLeastOnce()
      .returning("blah-url")

    (servicesConfig
      .getBoolean(_: String))
      .expects("features.allowPayeAccess")
      .returning(allowPayeAccess)
    (servicesConfig
      .getBoolean(_: String))
      .expects("features.enable-granular-permissions")
      .returning(true)
      .anyNumberOfTimes()

    servicesConfig
  }
}
