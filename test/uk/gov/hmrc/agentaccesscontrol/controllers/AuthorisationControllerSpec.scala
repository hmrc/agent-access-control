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

import play.api.Environment
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitTest
import uk.gov.hmrc.agentaccesscontrol.mocks.connectors.MockAuthConnector
import uk.gov.hmrc.agentaccesscontrol.mocks.services.{
  MockAuthorisationService,
  MockESAuthorisationService
}
import uk.gov.hmrc.agentaccesscontrol.models.{AccessResponse, AuthDetails}
import uk.gov.hmrc.agentmtdidentifiers.model.{
  Arn,
  CbcId,
  CgtRef,
  MtdItId,
  PptRef,
  Service,
  Urn,
  Utr,
  Vrn
}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{Nino => _, _}
import uk.gov.hmrc.domain.{
  AgentCode,
  EmpRef,
  Nino,
  SaAgentReference,
  SaUtr,
  TaxIdentifier
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisationControllerSpec
    extends UnitTest
    with MockAuthorisationService
    with MockAuthConnector
    with MockESAuthorisationService {

  private val cc: ControllerComponents = stubControllerComponents()
  private val environment: Environment = mock[Environment]
  private val arn: Arn = Arn("arn")
  private val agentCode = "ABCDEF123456"
  private val credentialRole: User.type = User

  def controller(): AuthorisationController = {
    new AuthorisationController(mockAuthorisationService,
                                mockAuthConnector,
                                mockESAuthorisationService,
                                environment,
                                cc)(global)
  }

  private val mtdAuthDetails: AuthDetails = AuthDetails(saAgentReference = None,
                                                        arn = Some(arn),
                                                        "ggId",
                                                        Some("Agent"),
                                                        Some(User))

  private val agentEnrolment: Set[Enrolment] = Set(
    Enrolment("HMRC-AS-AGENT",
              Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
              state = "Active",
              delegatedAuthRule = None))

  private val ggCredentials: Credentials =
    Credentials("ggId", "GovernmentGateway")

  private val authResponseMtdAgent
    : Future[~[~[~[Option[String], Enrolments], Option[CredentialRole]],
               Option[Credentials]]] =
    Future successful new ~(
      new ~(new ~(Some(agentCode), Enrolments(agentEnrolment)),
            Some(credentialRole)),
      Some(ggCredentials))

  private val saAuthDetails: AuthDetails = AuthDetails(
    saAgentReference = Some(SaAgentReference("enrol-123")),
    arn = None,
    ggCredentialId = "ggId",
    affinityGroup = Some("Agent"),
    agentUserRole = Some(credentialRole)
  )

  private val saAgentEnrolment: Set[Enrolment] = Set(
    Enrolment("IR-SA-AGENT",
              Seq(EnrolmentIdentifier("IRAgentReference", "enrol-123")),
              state = "Active",
              delegatedAuthRule = None))

  private val authResponseSaAgent
    : Future[~[~[~[Option[String], Enrolments], Option[CredentialRole]],
               Option[Credentials]]] =
    Future successful new ~(
      new ~(new ~(Some(agentCode), Enrolments(saAgentEnrolment)),
            Some(credentialRole)),
      Some(ggCredentials))

  "AuthoriseController" when {

    //Special Cases
    "provided with epaye-auth" should {
      "be routed correctly and handle an 'Authorised' response" in {
        mockAuthorise(authResponseMtdAgent)
        mockIsAuthorisedForPaye(AgentCode(agentCode),
                                EmpRef("123", "123456"),
                                mtdAuthDetails,
                                Future.successful(AccessResponse.Authorised))

        val response =
          controller().authorise("epaye-auth",
                                 agentCode,
                                 EmpRef("123", "123456").value)(FakeRequest())

        status(response) shouldBe Status.OK
      }
      "be routed correctly and handle a 'NoAssignment' response" in {
        mockAuthorise(authResponseMtdAgent)
        mockIsAuthorisedForPaye(AgentCode(agentCode),
                                EmpRef("123", "123456"),
                                mtdAuthDetails,
                                Future.successful(AccessResponse.NoAssignment))

        val response =
          controller().authorise("epaye-auth",
                                 agentCode,
                                 EmpRef("123", "123456").value)(FakeRequest())

        status(response) shouldBe Status.UNAUTHORIZED
      }
      "be routed correctly and handle a 'NoRelationship' response" in {
        mockAuthorise(authResponseMtdAgent)
        mockIsAuthorisedForPaye(
          AgentCode(agentCode),
          EmpRef("123", "123456"),
          mtdAuthDetails,
          Future.successful(AccessResponse.NoRelationship))

        val response =
          controller().authorise("epaye-auth",
                                 agentCode,
                                 EmpRef("123", "123456").value)(FakeRequest())

        status(response) shouldBe Status.UNAUTHORIZED
      }
    }

    "provided with sa-auth" should {
      "be routed correctly and handle an 'Authorised' response" in {
        mockAuthorise(authResponseSaAgent)
        mockIsAuthorisedForSa(AgentCode(agentCode),
                              SaUtr("utr"),
                              saAuthDetails,
                              Future.successful(AccessResponse.Authorised))

        val response =
          controller().authorise("sa-auth", agentCode, "utr")(FakeRequest())

        status(response) shouldBe Status.OK
      }
      "be routed correctly and handle a 'NoAssignment' response" in {
        mockAuthorise(authResponseSaAgent)
        mockIsAuthorisedForSa(AgentCode(agentCode),
                              SaUtr("utr"),
                              saAuthDetails,
                              Future.successful(AccessResponse.NoAssignment))

        val response =
          controller().authorise("sa-auth", agentCode, "utr")(FakeRequest())

        status(response) shouldBe Status.UNAUTHORIZED
      }
      "be routed correctly and handle a 'NoRelationship' response" in {
        mockAuthorise(authResponseSaAgent)
        mockIsAuthorisedForSa(AgentCode(agentCode),
                              SaUtr("utr"),
                              saAuthDetails,
                              Future.successful(AccessResponse.NoRelationship))

        val response =
          controller().authorise("sa-auth", agentCode, "utr")(FakeRequest())

        status(response) shouldBe Status.UNAUTHORIZED
      }
    }

    "provided with afi-auth" should {
      "be routed correctly and handle an 'Authorised' response" in {
        mockAuthorise(authResponseMtdAgent)
        mockIsAuthorisedForAfi(AgentCode(agentCode),
                               Nino("AA123456A"),
                               mtdAuthDetails,
                               Future.successful(AccessResponse.Authorised))

        val response =
          controller().authorise("afi-auth", agentCode, "AA123456A")(
            FakeRequest())

        status(response) shouldBe Status.OK
      }
      "be routed correctly and handle a 'NoAssignment' response" in {
        mockAuthorise(authResponseMtdAgent)
        mockIsAuthorisedForAfi(AgentCode(agentCode),
                               Nino("AA123456A"),
                               mtdAuthDetails,
                               Future.successful(AccessResponse.NoAssignment))

        val response =
          controller().authorise("afi-auth", agentCode, "AA123456A")(
            FakeRequest())

        status(response) shouldBe Status.UNAUTHORIZED
      }
      "be routed correctly and handle a 'NoRelationship' response" in {
        mockAuthorise(authResponseMtdAgent)
        mockIsAuthorisedForAfi(AgentCode(agentCode),
                               Nino("AA123456A"),
                               mtdAuthDetails,
                               Future.successful(AccessResponse.NoRelationship))

        val response =
          controller().authorise("afi-auth", agentCode, "AA123456A")(
            FakeRequest())

        status(response) shouldBe Status.UNAUTHORIZED
      }
    }

    //Standard Cases
    val templateTestDataSets: Seq[(String, TaxIdentifier, Service)] = Seq(
      ("mtd-it-auth", MtdItId("1234567890"), Service.MtdIt),
      ("mtd-vat-auth", Vrn("123456789"), Service.Vat),
      ("trust-auth", Utr("0123456789"), Service.Trust),
      ("trust-auth", Urn("xxtrust12345678"), Service.TrustNT),
      ("cgt-auth", CgtRef("XMCGTP123456789"), Service.CapitalGains),
      ("ppt-auth", PptRef("XAPPT0000123456"), Service.Ppt),
      ("cbc-auth", CbcId("XACBC0123456789"), Service.Cbc),
    )

    templateTestDataSets.foreach(testData =>
      s"provided with ${testData._3}" should {
        "be routed correctly and handle an 'Authorised' response" in {
          mockAuthorise(authResponseMtdAgent)
          mockAuthoriseStandardService(
            AgentCode(agentCode),
            testData._2,
            testData._3.id,
            mtdAuthDetails,
            Future.successful(AccessResponse.Authorised))

          val response =
            controller().authorise(testData._1, agentCode, testData._2.value)(
              FakeRequest())

          status(response) shouldBe Status.OK
        }
        "be routed correctly and handle a 'NoAssignment' response" in {
          mockAuthorise(authResponseMtdAgent)
          mockAuthoriseStandardService(
            AgentCode(agentCode),
            testData._2,
            testData._3.id,
            mtdAuthDetails,
            Future.successful(AccessResponse.NoAssignment))

          val response =
            controller().authorise(testData._1, agentCode, testData._2.value)(
              FakeRequest())

          status(response) shouldBe Status.UNAUTHORIZED
        }
        "be routed correctly and handle a 'NoRelationship' response" in {
          mockAuthorise(authResponseMtdAgent)
          mockAuthoriseStandardService(
            AgentCode(agentCode),
            testData._2,
            testData._3.id,
            mtdAuthDetails,
            Future.successful(AccessResponse.NoRelationship))

          val response =
            controller().authorise(testData._1, agentCode, testData._2.value)(
              FakeRequest())

          status(response) shouldBe Status.UNAUTHORIZED
        }
        "handle exception in authorisations service" in {
          mockAuthorise(authResponseMtdAgent)
          mockAuthoriseStandardService(
            AgentCode(agentCode),
            testData._2,
            testData._3.id,
            mtdAuthDetails,
            Future.failed(
              new IllegalArgumentException(s"Unexpected auth type: x")))

          val response =
            controller().authorise(testData._1, agentCode, testData._2.value)(
              FakeRequest())

          status(response) shouldBe Status.BAD_REQUEST
        }
    })

    //Misc. cases
    "provided with an invalid auth type" should {
      "return a bad request" in { //TODO code needs fixing, this should be bad request
        mockAuthorise(authResponseMtdAgent)

        an[IllegalArgumentException] shouldBe thrownBy(
          status(
            controller().authorise("invalid-auth-type", agentCode, "utr")(
              FakeRequest()))
        )
      }
    }

    "provided with an invalid trust identifier" should {
      "return a bad request" in { //TODO code needs fixing, this should be bad request
        mockAuthorise(authResponseMtdAgent)

        an[IllegalArgumentException] shouldBe thrownBy(
          status(
            controller().authorise("trust-auth", agentCode, "XMCGTP123456789")(
              FakeRequest()))
        )
      }
    }

    "auth returns an UnsupportedAffinityGroup" should {
      "return a Forbidden response" in {
        mockAuthorise(
          Future.failed(UnsupportedAffinityGroup("UnsupportedAffinityGroup")))

        val response =
          controller().authorise("authType", agentCode, "clientId")(
            FakeRequest())

        status(response) shouldBe Status.FORBIDDEN
      }
    }

    "auth returns an UnsupportedAuthProvider" should {
      "return a Forbidden response" in {
        mockAuthorise(
          Future.failed(UnsupportedAuthProvider("UnsupportedAuthProvider")))

        val response =
          controller().authorise("authType", agentCode, "clientId")(
            FakeRequest())

        status(response) shouldBe Status.FORBIDDEN
      }
    }

  }

}
