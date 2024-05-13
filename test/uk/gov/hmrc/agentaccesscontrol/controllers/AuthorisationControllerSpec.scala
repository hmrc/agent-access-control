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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.agentaccesscontrol.models.AccessResponse
import uk.gov.hmrc.agentaccesscontrol.models.AuthDetails
import uk.gov.hmrc.agentaccesscontrol.services.AuthorisationService
import uk.gov.hmrc.agentaccesscontrol.services.ESAuthorisationService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.CbcId
import uk.gov.hmrc.agentmtdidentifiers.model.CgtRef
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentmtdidentifiers.model.PlrId
import uk.gov.hmrc.agentmtdidentifiers.model.PptRef
import uk.gov.hmrc.agentmtdidentifiers.model.Service
import uk.gov.hmrc.agentmtdidentifiers.model.Urn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.auth.core.{ Nino => _, _ }
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

class AuthorisationControllerSpec extends UnitSpec {

  private val cc: ControllerComponents  = stubControllerComponents()
  private val arn: Arn                  = Arn("arn")
  private val agentCode                 = "ABCDEF123456"
  private val credentialRole: User.type = User

  trait Setup {
    protected val mockAuthorisationService: AuthorisationService =
      mock[AuthorisationService]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockESAuthorisationService: ESAuthorisationService =
      mock[ESAuthorisationService]

    object TestController
        extends AuthorisationController(mockAuthorisationService, mockAuthConnector, mockESAuthorisationService, cc)
  }

  private val mtdAuthDetails: AuthDetails =
    AuthDetails(saAgentReference = None, arn = Some(arn), "ggId", Some("Agent"), Some(User))

  private val agentEnrolment: Set[Enrolment] = Set(
    Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None
    )
  )

  private val ggCredentials: Credentials =
    Credentials("ggId", "GovernmentGateway")

  private val authResponseMtdAgent
      : Future[~[~[~[Option[String], Enrolments], Option[CredentialRole]], Option[Credentials]]] =
    Future.successful(
      new ~(new ~(new ~(Some(agentCode), Enrolments(agentEnrolment)), Some(credentialRole)), Some(ggCredentials))
    )

  private val saAuthDetails: AuthDetails = AuthDetails(
    saAgentReference = Some(SaAgentReference("enrol-123")),
    arn = None,
    ggCredentialId = "ggId",
    affinityGroup = Some("Agent"),
    agentUserRole = Some(credentialRole)
  )

  private val saAgentEnrolment: Set[Enrolment] = Set(
    Enrolment(
      "IR-SA-AGENT",
      Seq(EnrolmentIdentifier("IRAgentReference", "enrol-123")),
      state = "Active",
      delegatedAuthRule = None
    )
  )

  private val authResponseSaAgent
      : Future[~[~[~[Option[String], Enrolments], Option[CredentialRole]], Option[Credentials]]] =
    Future.successful(
      new ~(new ~(new ~(Some(agentCode), Enrolments(saAgentEnrolment)), Some(credentialRole)), Some(ggCredentials))
    )

  private implicit val ec: ExecutionContext =
    concurrent.ExecutionContext.Implicits.global

  "AuthoriseController" when {

    // Special Cases
    "provided with epaye-auth" should {
      "be routed correctly and handle an 'Authorised' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        mockAuthorisationService
          .isAuthorisedForPaye(AgentCode(agentCode), EmpRef("123", "123456"), mtdAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.Authorised))

        val response: Future[Result] =
          TestController.authorise("epaye-auth", agentCode, EmpRef("123", "123456").value)(FakeRequest())

        status(response) mustBe Status.OK
      }
      "be routed correctly and handle a 'NoAssignment' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        mockAuthorisationService
          .isAuthorisedForPaye(AgentCode(agentCode), EmpRef("123", "123456"), mtdAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.NoAssignment))

        val response: Future[Result] =
          TestController.authorise("epaye-auth", agentCode, EmpRef("123", "123456").value)(FakeRequest())

        status(response) mustBe Status.UNAUTHORIZED
      }
      "be routed correctly and handle a 'NoRelationship' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        mockAuthorisationService
          .isAuthorisedForPaye(AgentCode(agentCode), EmpRef("123", "123456"), mtdAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.NoRelationship))

        val response: Future[Result] =
          TestController.authorise("epaye-auth", agentCode, EmpRef("123", "123456").value)(FakeRequest())

        status(response) mustBe Status.UNAUTHORIZED
      }
    }

    "provided with sa-auth" should {
      "be routed correctly and handle an 'Authorised' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseSaAgent)

        mockAuthorisationService
          .isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"), saAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.Authorised))

        val response: Future[Result] =
          TestController.authorise("sa-auth", agentCode, "utr")(FakeRequest())

        status(response) mustBe Status.OK
      }
      "be routed correctly and handle a 'NoAssignment' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseSaAgent)
        mockAuthorisationService
          .isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"), saAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.NoAssignment))

        val response: Future[Result] =
          TestController.authorise("sa-auth", agentCode, "utr")(FakeRequest())

        status(response) mustBe Status.UNAUTHORIZED
      }
      "be routed correctly and handle a 'NoRelationship' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseSaAgent)
        mockAuthorisationService
          .isAuthorisedForSa(AgentCode(agentCode), SaUtr("utr"), saAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.NoRelationship))

        val response: Future[Result] =
          TestController.authorise("sa-auth", agentCode, "utr")(FakeRequest())

        status(response) mustBe Status.UNAUTHORIZED
      }
    }

    "provided with afi-auth" should {
      "be routed correctly and handle an 'Authorised' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        mockAuthorisationService
          .isAuthorisedForAfi(AgentCode(agentCode), Nino("AA123456A"), mtdAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.Authorised))

        val response: Future[Result] =
          TestController.authorise("afi-auth", agentCode, "AA123456A")(FakeRequest())

        status(response) mustBe Status.OK
      }
      "be routed correctly and handle a 'NoAssignment' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        mockAuthorisationService
          .isAuthorisedForAfi(AgentCode(agentCode), Nino("AA123456A"), mtdAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.NoAssignment))

        val response: Future[Result] =
          TestController.authorise("afi-auth", agentCode, "AA123456A")(FakeRequest())

        status(response) mustBe Status.UNAUTHORIZED
      }
      "be routed correctly and handle a 'NoRelationship' response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        mockAuthorisationService
          .isAuthorisedForAfi(AgentCode(agentCode), Nino("AA123456A"), mtdAuthDetails)(
            *[ExecutionContext],
            *[HeaderCarrier],
            *[Request[Any]]
          )
          .returns(Future.successful(AccessResponse.NoRelationship))

        val response: Future[Result] =
          TestController.authorise("afi-auth", agentCode, "AA123456A")(FakeRequest())

        status(response) mustBe Status.UNAUTHORIZED
      }
    }

    // Standard Cases
    val templateTestDataSets: Seq[(String, TaxIdentifier, Service)] = Seq(
      ("mtd-it-auth", MtdItId("1234567890"), Service.MtdIt),
      ("mtd-vat-auth", Vrn("123456789"), Service.Vat),
      ("trust-auth", Utr("0123456789"), Service.Trust),
      ("trust-auth", Urn("xxtrust12345678"), Service.TrustNT),
      ("cgt-auth", CgtRef("XMCGTP123456789"), Service.CapitalGains),
      ("ppt-auth", PptRef("XAPPT0000123456"), Service.Ppt),
      ("cbc-auth", CbcId("XACBC0123456789"), Service.Cbc),
      ("pillar2-auth", PlrId("XDPLR6210917659"), Service.Pillar2),
    )

    templateTestDataSets.foreach(testData =>
      s"provided with ${testData._3}" should {
        "be routed correctly and handle an 'Authorised' response" in new Setup {
          mockAuthConnector
            .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
            .returns(authResponseMtdAgent)

          mockESAuthorisationService
            .authoriseStandardService(AgentCode(agentCode), testData._2, testData._3.id, mtdAuthDetails)(
              *[HeaderCarrier],
              *[Request[Any]]
            )
            .returns(Future.successful(AccessResponse.Authorised))

          val response: Future[Result] =
            TestController.authorise(testData._1, agentCode, testData._2.value)(FakeRequest())

          status(response) mustBe Status.OK
        }
        "be routed correctly and handle a 'NoAssignment' response" in new Setup {
          mockAuthConnector
            .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
            .returns(authResponseMtdAgent)
          mockESAuthorisationService
            .authoriseStandardService(AgentCode(agentCode), testData._2, testData._3.id, mtdAuthDetails)(
              *[HeaderCarrier],
              *[Request[Any]]
            )
            .returns(Future.successful(AccessResponse.NoAssignment))

          val response: Future[Result] =
            TestController.authorise(testData._1, agentCode, testData._2.value)(FakeRequest())

          status(response) mustBe Status.UNAUTHORIZED
        }
        "be routed correctly and handle a 'NoRelationship' response" in new Setup {
          mockAuthConnector
            .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
            .returns(authResponseMtdAgent)
          mockESAuthorisationService
            .authoriseStandardService(AgentCode(agentCode), testData._2, testData._3.id, mtdAuthDetails)(
              *[HeaderCarrier],
              *[Request[Any]]
            )
            .returns(Future.successful(AccessResponse.NoRelationship))

          val response: Future[Result] =
            TestController.authorise(testData._1, agentCode, testData._2.value)(FakeRequest())

          status(response) mustBe Status.UNAUTHORIZED
        }
        "handle exception in authorisations service" in new Setup {
          mockAuthConnector
            .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
            .returns(authResponseMtdAgent)
          mockESAuthorisationService
            .authoriseStandardService(AgentCode(agentCode), testData._2, testData._3.id, mtdAuthDetails)(
              *[HeaderCarrier],
              *[Request[Any]]
            )
            .returns(Future.failed(new IllegalArgumentException(s"Unexpected auth type: x")))

          val response: Future[Result] =
            TestController.authorise(testData._1, agentCode, testData._2.value)(FakeRequest())

          status(response) mustBe Status.BAD_REQUEST
        }
      }
    )

    // Misc. cases
    "provided with an invalid auth type" should {
      "return a bad request" in new Setup { // TODO code needs fixing, this should be bad request
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        an[IllegalArgumentException] mustBe thrownBy(
          status(TestController.authorise("invalid-auth-type", agentCode, "utr")(FakeRequest()))
        )
      }
    }

    "provided with an invalid trust identifier" should {
      "return a bad request" in new Setup { // TODO code needs fixing, this should be bad request
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(authResponseMtdAgent)

        an[IllegalArgumentException] mustBe thrownBy(
          status(TestController.authorise("trust-auth", agentCode, "XMCGTP123456789")(FakeRequest()))
        )
      }
    }

    "auth returns an UnsupportedAffinityGroup" should {
      "return a Forbidden response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(Future.failed(UnsupportedAffinityGroup("UnsupportedAffinityGroup")))

        val response: Future[Result] =
          TestController.authorise("authType", agentCode, "clientId")(FakeRequest())

        status(response) mustBe Status.FORBIDDEN
      }
    }

    "auth returns an UnsupportedAuthProvider" should {
      "return a Forbidden response" in new Setup {
        mockAuthConnector
          .authorise(*[Predicate], *[Retrieval[Any]])(*[HeaderCarrier], *[ExecutionContext])
          .returns(Future.failed(UnsupportedAuthProvider("UnsupportedAuthProvider")))

        val response: Future[Result] =
          TestController.authorise("authType", agentCode, "clientId")(FakeRequest())

        status(response) mustBe Status.FORBIDDEN
      }
    }

  }

}
