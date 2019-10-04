/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Provider
import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.connectors.{AgentAccessAuthConnector, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, ESAuthorisationService}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.auth.core.{Admin, CredentialRole, Enrolment, EnrolmentIdentifier, Enrolments, PlayAuthConnector}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class AuthorisationControllerSpec
    extends UnitSpec
    with BeforeAndAfterEach
    with MockitoSugar {

  val authorisationService = mock[AuthorisationService]
  val esAuthorisationService = mock[ESAuthorisationService]
  val mockPlayAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  val ecp: Provider[ExecutionContextExecutor] =
    new Provider[ExecutionContextExecutor] {
      override def get(): ExecutionContextExecutor =
        concurrent.ExecutionContext.Implicits.global
    }
  val environment = mock[Environment]
  val arn = Arn("arn")
  val agentCode = "ABCDEF123456"
  val credentialRole = Admin
  val providerId = "12345-credId"


  def controller(enabled: Boolean = true) =
    new AuthorisationController(
      authorisationService,
      mockPlayAuthConnector,
      esAuthorisationService,
      Configuration("features.allowPayeAccess" -> enabled),
      environment,
      ecp)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(authorisationService)
    reset(mockPlayAuthConnector)
    reset(esAuthorisationService)
  }

  val agentEnrolment = Set(
    Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None))

  val ggCredentials = Credentials("ggId", "GovernmentGateway")

  val authResponseMtdAgent: Future[~[~[~[Option[String], Enrolments],Option[CredentialRole]], Option[Credentials]]] =
    Future successful new ~(new ~(new ~(Some(agentCode), Enrolments(agentEnrolment)), Some(credentialRole)), Some(ggCredentials))

  val saAgentReference = SaAgentReference("enrol-123")

  val nonMtdAuthDetails = AuthDetails(saAgentReference = Some(saAgentReference),
                                arn = None,
                                ggCredentialId = "12345-credId",
                                affinityGroup = Some("Agent"),
                                agentUserRole = Some(credentialRole))

  val mtdAuthDetails = AuthDetails(saAgentReference = None,
    arn = Some(arn), "ggId", Some("Agent"), Some(Admin)
  )

  private def anSaEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {

    "return 401 if the AuthorisationService doesn't permit access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails) thenReturn (Future successful false)

      val response =
        controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the AuthorisationService allows access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails) thenReturn (Future successful true)

      val response =
        controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "pass request to AuthorisationService" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails) thenReturn (Future successful true)

      val response =
        await(controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(fakeRequest))
      verify(authorisationService)
        .isAuthorisedForSa(any[AgentCode], any[SaUtr], eqs(mtdAuthDetails))(
          any[ExecutionContext],
          any[HeaderCarrier],
          any[Request[Any]])
      status(response) shouldBe Status.OK
    }
    "propagate exception if the AuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails) thenReturn (Future failed new IllegalStateException(
        "some error"))
      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"))(
          fakeRequest)))
    }
  }

  private def anMdtitEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 401 if the MtdAuthorisationService doesn't permit access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdItAuthorisationServiceIsCalled thenReturn (Future successful false)

      val response =
        controller().isAuthorisedForMtdIt(AgentCode(agentCode), MtdItId("mtdItId"))(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the MtdAuthorisationService allows access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdItAuthorisationServiceIsCalled thenReturn (Future successful true)

      val response =
        controller().isAuthorisedForMtdIt(AgentCode(agentCode), MtdItId("mtdItId"))(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    "propagate exception if the MtdAuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdItAuthorisationServiceIsCalled thenReturn (Future failed new IllegalStateException(
        "some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(
          controller().isAuthorisedForMtdIt(AgentCode(agentCode), MtdItId("mtdItId"))(
            fakeRequest)))
    }
  }

  private def anMtdVatEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 401 if the MtdVatAuthorisationService doesn't permit access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdVatAuthorisationServiceIsCalled thenReturn (Future successful false)

      val response = controller().isAuthorisedForMtdVat(AgentCode(agentCode),
                                                        Vrn("vrn"))(fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the MtdVatAuthorisationService allows access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdVatAuthorisationServiceIsCalled thenReturn (Future successful true)

      val response = controller().isAuthorisedForMtdVat(AgentCode(agentCode),
                                                        Vrn("vrn"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "propagate exception if the MtdVatAuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenMtdVatAuthorisationServiceIsCalled thenReturn (Future failed new IllegalStateException(
        "some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForMtdVat(AgentCode(agentCode), Vrn("vrn"))(
          fakeRequest)))
    }
  }

  private def aPayeEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {
    "return 200 when Paye is enabled" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPayeAuthorisationServiceIsCalled thenReturn (Future successful true)

      val response =
        controller().isAuthorisedForPaye(AgentCode(agentCode),
                                         EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 200
    }

    "return 403 when Paye is disabled" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPayeAuthorisationServiceIsCalled thenReturn (Future successful true)

      val response =
        controller(enabled = false).isAuthorisedForPaye(
          AgentCode(agentCode),
          EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 403
    }
  }

  private def anAfiEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]) = {

    "return 200 if the AuthorisationService allows access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled thenReturn (Future successful true)

      val response =
        controller().isAuthorisedForAfi(AgentCode(agentCode), Nino("AA123456A"))(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return 401 if the AuthorisationService does not allow access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled thenReturn (Future successful false)

      val response =
        controller().isAuthorisedForAfi(AgentCode(agentCode), Nino("AA123456A"))(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "propagate exception if the AuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled thenReturn (Future failed new IllegalStateException(
        "some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(controller().isAuthorisedForAfi(AgentCode(agentCode),
                                               Nino("AA123456A"))(fakeRequest)))
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
    behave like anMdtitEndpoint(
      FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent//client/utr"))
  }

  "POST isAuthorisedForMtdIt" should {
    behave like anMdtitEndpoint(
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

  def whenAuthIsCalled(returnValue: Future[~[~[~[Option[String], Enrolments],Option[CredentialRole]], Option[Credentials]]]) =
  when(
    mockPlayAuthConnector
      .authorise(any[Predicate](), any[Retrieval[~[~[~[Option[String], Enrolments], Option[CredentialRole]], Option[Credentials]]]]())(
        any[HeaderCarrier](),
        any[ExecutionContext]()))
    .thenReturn(returnValue)

  def whenAfiAuthorisationServiceIsCalled =
    when(
      authorisationService
        .isAuthorisedForAfi(any[AgentCode], any[Nino], eqs(mtdAuthDetails))(
          any[ExecutionContext],
          any[HeaderCarrier],
          any[Request[Any]]))

  def whenAuthorisationServiceIsCalled(authDetails: AuthDetails) =
    when(
      authorisationService
        .isAuthorisedForSa(any[AgentCode], any[SaUtr], eqs(authDetails))(
          any[ExecutionContext],
          any[HeaderCarrier],
          any[Request[Any]]))

  def whenMtdItAuthorisationServiceIsCalled =
    when(
      esAuthorisationService
        .authoriseForMtdIt(any[AgentCode], any[MtdItId], eqs(mtdAuthDetails))(
          any[HeaderCarrier],
          any[Request[_]]))

  def whenMtdVatAuthorisationServiceIsCalled =
    when(
      esAuthorisationService
        .authoriseForMtdVat(any[AgentCode], any[Vrn], eqs(mtdAuthDetails))(
          any[HeaderCarrier],
          any[Request[_]]))

  def whenPayeAuthorisationServiceIsCalled =
    when(
      authorisationService
        .isAuthorisedForPaye(any[AgentCode], any[EmpRef], eqs(mtdAuthDetails))(
          any[ExecutionContext],
          any[HeaderCarrier],
          any[Request[_]]))
}

