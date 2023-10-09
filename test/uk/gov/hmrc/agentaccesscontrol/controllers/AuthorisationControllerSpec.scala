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
import uk.gov.hmrc.agentaccesscontrol.model.{AccessResponse, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.service.{
  AuthorisationService,
  ESAuthorisationService
}
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Service}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{Nino => _, _}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerSpec
    extends UnitSpec
    with BeforeAndAfterEach
    with MockFactory {

  val authorisationService: AuthorisationService = mock[AuthorisationService]
  val esAuthorisationService: ESAuthorisationService =
    mock[ESAuthorisationService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val cc: ControllerComponents = stubControllerComponents()
  val environment: Environment = mock[Environment]
  val arn: Arn = Arn("arn")
  val agentCode = "ABCDEF123456"
  val credentialRole: User.type = User
  val providerId = "12345-credId"

  def controller(): AuthorisationController = {
    new AuthorisationController(authorisationService,
                                mockAuthConnector,
                                esAuthorisationService,
                                environment,
                                cc)(global)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  val agentEnrolment = Set(
    Enrolment("HMRC-AS-AGENT",
              Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
              state = "Active",
              delegatedAuthRule = None))

  val ggCredentials: Credentials = Credentials("ggId", "GovernmentGateway")

  val authResponseMtdAgent
    : Future[~[~[~[Option[String], Enrolments], Option[CredentialRole]],
               Option[Credentials]]] =
    Future successful new ~(
      new ~(new ~(Some(agentCode), Enrolments(agentEnrolment)),
            Some(credentialRole)),
      Some(ggCredentials))

  val saAgentReference: SaAgentReference = SaAgentReference("enrol-123")

  val nonMtdAuthDetails: AuthDetails = AuthDetails(
    saAgentReference = Some(saAgentReference),
    arn = None,
    ggCredentialId = "12345-credId",
    affinityGroup = Some("Agent"),
    agentUserRole = Some(credentialRole))

  val mtdAuthDetails: AuthDetails = AuthDetails(saAgentReference = None,
                                                arn = Some(arn),
                                                "ggId",
                                                Some("Agent"),
                                                Some(User))

  private def anSaEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]): Unit = {

    "return 401 if the AuthorisationService doesn't permit access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future.successful(AccessResponse.NoRelationship))

      val response =
        controller().authorise("sa-auth", AgentCode(agentCode), "utr")(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the AuthorisationService allows access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future.successful(AccessResponse.Authorised))

      val response =
        controller().authorise("sa-auth", AgentCode(agentCode), "utr")(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    "pass request to AuthorisationService" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future.successful(AccessResponse.Authorised))

      val response = await(
        controller().authorise("sa-auth", AgentCode(agentCode), "utr")(
          fakeRequest))
      status(response) shouldBe Status.OK
    }
    "propagate exception if the AuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAuthorisationServiceIsCalled(mtdAuthDetails).returning(
        Future failed new IllegalStateException("some error"))
      an[IllegalStateException] shouldBe thrownBy(
        status(controller().authorise("sa-auth", AgentCode(agentCode), "utr")(
          fakeRequest)))
    }
  }

  private def aPayeEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]): Unit = {
    "return 200 when Paye is enabled" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenPayeAuthorisationServiceIsCalled.returning(
        Future.successful(AccessResponse.Authorised))

      val response =
        controller().authorise("epaye-auth",
                               AgentCode(agentCode),
                               EmpRef("123", "123456").value)(fakeRequest)

      status(response) shouldBe 200
    }
  }

  private def anAfiEndpoint(fakeRequest: FakeRequest[_ <: AnyContent]): Unit = {

    "return 200 if the AuthorisationService allows access" in {

      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled.returning(
        Future.successful(AccessResponse.Authorised))

      val response =
        controller().authorise("afi-auth", AgentCode(agentCode), "AA123456A")(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return 401 if the AuthorisationService does not allow access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled.returning(
        Future.successful(AccessResponse.NoRelationship))

      val response =
        controller().authorise("afi-auth", AgentCode(agentCode), "AA123456A")(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    "propagate exception if the AuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenAfiAuthorisationServiceIsCalled.returning(
        Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(
          controller().authorise("afi-auth", AgentCode(agentCode), "AA123456A")(
            fakeRequest)))
    }

  }

  private def authEndpointBehaviours(authType: String,
                                     service: Service,
                                     clientId: String): Unit = {
    val fakeRequest = FakeRequest()

    s"return 401 if the EsAuthorisationService doesn't permit access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenEsAuthorisationServiceIsCalledFor(service)
        .returning(Future.successful(AccessResponse.NoRelationship))

      val response =
        controller().authorise(authType, AgentCode(agentCode), clientId)(
          fakeRequest)

      status(response) shouldBe Status.UNAUTHORIZED
    }

    s"return 200 if the EsAuthorisationService allows access" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenEsAuthorisationServiceIsCalledFor(service)
        .returning(Future.successful(AccessResponse.Authorised))

      val response =
        controller().authorise(authType, AgentCode(agentCode), clientId)(
          fakeRequest)

      status(response) shouldBe Status.OK
    }

    s"propagate exception if the EsAuthorisationService fails" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenEsAuthorisationServiceIsCalledFor(service)
        .returning(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(
        status(
          controller().authorise(authType, AgentCode(agentCode), clientId)(
            fakeRequest))
      )
    }
  }

  // Services with specific setup required:

  "GET isAuthorisedForSa" should {
    behave like anSaEndpoint(
      FakeRequest("GET", "/agent-access-control/sa-auth/agent//client/utr"))
  }

  "POST isAuthorisedForSa" should {
    behave like anSaEndpoint(
      FakeRequest("POST", "/agent-access-control/sa-auth/agent/client/utr")
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

  // 'standard' (ES) services:

  "authorise (trust)" should {
    behave like authEndpointBehaviours("trust-auth",
                                       Service.Trust,
                                       "0123456789")
    // additional check for trust
    s"not authorise if the relationship is for trust (taxable) but a URN is provided" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenEsAuthorisationServiceIsCalledFor(Service.Trust)
        .returning(Future.successful(AccessResponse.Authorised))
        .anyNumberOfTimes
      whenEsAuthorisationServiceIsCalledFor(Service.TrustNT)
        .returning(Future.successful(AccessResponse.NoRelationship))
        .anyNumberOfTimes

      val response =
        controller().authorise("trust-auth",
                               AgentCode(agentCode),
                               "xxtrust12345678" /* URN */ )(FakeRequest())

      status(response) shouldBe Status.UNAUTHORIZED
    }
  }

  "authorise (trust NT)" should {
    behave like authEndpointBehaviours("trust-auth",
                                       Service.TrustNT,
                                       "xxtrust12345678")
    // additional check for trust
    s"not authorise if the relationship is for trust (non-taxable) but a UTR is provided" in {
      whenAuthIsCalled(authResponseMtdAgent)
      whenEsAuthorisationServiceIsCalledFor(Service.Trust)
        .returning(Future.successful(AccessResponse.NoRelationship))
        .anyNumberOfTimes
      whenEsAuthorisationServiceIsCalledFor(Service.TrustNT)
        .returning(Future.successful(AccessResponse.Authorised))
        .anyNumberOfTimes

      val response =
        controller().authorise("trust-auth",
                               AgentCode(agentCode),
                               "0123456789" /* UTR */ )(FakeRequest())

      status(response) shouldBe Status.UNAUTHORIZED
    }
  }

  "authorise (MTD IT)" should {
    behave like authEndpointBehaviours("mtd-it-auth",
                                       Service.MtdIt,
                                       "1234567890")
  }

  "authorise (VAT)" should {
    behave like authEndpointBehaviours("mtd-vat-auth", Service.Vat, "123456789")
  }

  "authorise (PPT)" should {
    behave like authEndpointBehaviours("ppt-auth",
                                       Service.Ppt,
                                       "XAPPT0000123456")
  }

  "authorise (CBC)" should {
    behave like authEndpointBehaviours("cbc-auth",
                                       Service.Cbc,
                                       "XACBC0123456789")
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

  def whenEsAuthorisationServiceIsCalledFor(service: Service) =
    (esAuthorisationService
      .authoriseStandardService(
        _: AgentCode,
        _: MtdItId,
        _: String,
        _: AuthDetails)(_: HeaderCarrier, _: Request[Any]))
      .expects(*, *, service.id, *, *, *)

  def whenPayeAuthorisationServiceIsCalled =
    (authorisationService
      .isAuthorisedForPaye(_: AgentCode, _: EmpRef, _: AuthDetails)(
        _: ExecutionContext,
        _: HeaderCarrier,
        _: Request[Any]))
      .expects(*, *, *, *, *, *)
}
