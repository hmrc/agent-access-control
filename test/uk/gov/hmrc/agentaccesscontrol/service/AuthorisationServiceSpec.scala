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

import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.audit.{
  AgentAccessControlDecision,
  AuditService
}
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.{
  AfiRelationshipConnector,
  MappingConnector
}
import uk.gov.hmrc.agentaccesscontrol.model.{AccessResponse, AuthDetails}
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.{
  BadRequestException,
  HeaderCarrier,
  UpstreamErrorResponse
}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class AuthorisationServiceSpec extends UnitSpec with MockitoSugar {
  val agentCode = AgentCode("ABCDEF123456")
  val saAgentRef = SaAgentReference("ABC456")
  val clientSaUtr = SaUtr("CLIENTSAUTR456")
  val empRef = EmpRef("123", "01234567")
  val nino: Nino = Nino("AA101010A")
  val providerId = "12345-credId"
  val arn: Arn = Arn("arn")

  val nonMtdAuthDetails =
    AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(User))

  val notEnrolledAuthDetails =
    AuthDetails(None, None, "ggId", Some("Agent"), Some(User))

  val afiAuthDetails = AuthDetails(None, Some(arn), "ggId", None, None)

  implicit val hc = HeaderCarrier()
  implicit val ec = concurrent.ExecutionContext.Implicits.global
  implicit val fakeRequest = FakeRequest(
    "GET",
    s"/agent-access-control/sa-auth/agent/$agentCode/client/$clientSaUtr")

  "isAuthorisedForSa" should {
    "return Error if SA agent reference cannot be found (as CESA cannot be checked)" in new Context {

      await(
        authorisationService.isAuthorisedForSa(
          agentCode,
          clientSaUtr,
          notEnrolledAuthDetails)) should matchPattern {
        case AccessResponse.Error(_) =>
      }
      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "sa",
        clientSaUtr,
        Seq("credId" -> "ggId",
            "accessGranted" -> false,
            "affinityGroup" -> "Agent",
            "agentUserRole" -> User)
      )(hc, fakeRequest, ec)
    }

    "return false if SA agent reference is found and CesaAuthorisationService returns false and Enrolment Store " +
      "Proxy Authorisation returns true" in new Context {

      whenESPIsCheckedForSaRelationship thenReturn Future.successful(true)
      whenCesaIsCheckedForSaRelationship thenReturn Future.successful(false)

      await(
        authorisationService.isAuthorisedForSa(
          agentCode,
          clientSaUtr,
          nonMtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "sa",
        clientSaUtr,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> false,
          "cesaResult" -> false,
          "enrolmentStoreResult" -> true,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "return true if SA agent reference is found and DesAuthorisationService returns true and Enrolment Store Proxy Authorisation returns true" in new Context {

      whenESPIsCheckedForSaRelationship thenReturn Future.successful(true)
      whenCesaIsCheckedForSaRelationship thenReturn Future.successful(true)

      await(
        authorisationService.isAuthorisedForSa(
          agentCode,
          clientSaUtr,
          nonMtdAuthDetails)) shouldBe AccessResponse.Authorised
      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "sa",
        clientSaUtr,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> true,
          "cesaResult" -> true,
          "enrolmentStoreResult" -> true,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "not hard code audited values" in new Context {
      whenESPIsCheckedForSaRelationship thenReturn Future.successful(true)
      whenCesaIsCheckedForSaRelationship thenReturn Future.successful(true)

      val authDetails =
        AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(User))

      await(
        authorisationService.isAuthorisedForSa(
          agentCode,
          clientSaUtr,
          authDetails)) shouldBe AccessResponse.Authorised
      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "sa",
        clientSaUtr,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> true,
          "cesaResult" -> true,
          "enrolmentStoreResult" -> true,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "still work if the fields only used for auditing are removed from the auth record" in new Context {

      whenESPIsCheckedForSaRelationship thenReturn Future.successful(true)
      whenCesaIsCheckedForSaRelationship thenReturn Future.successful(true)

      await(
        authorisationService.isAuthorisedForSa(
          agentCode,
          clientSaUtr,
          nonMtdAuthDetails)) shouldBe AccessResponse.Authorised
      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "sa",
        clientSaUtr,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> true,
          "cesaResult" -> true,
          "enrolmentStoreResult" -> true,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "return false without calling DES if Enrolment Store Proxy Authorisation returns false (to reduce the load on DES)" in new Context {

      whenESPIsCheckedForSaRelationship thenReturn Future.successful(false)
      whenCesaIsCheckedForSaRelationship thenAnswer failBecauseDesShouldNotBeCalled

      await(
        authorisationService.isAuthorisedForSa(
          agentCode,
          clientSaUtr,
          nonMtdAuthDetails)) shouldBe AccessResponse.NoRelationship
      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "sa",
        clientSaUtr,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> false,
          "cesaResult" -> "notChecked",
          "enrolmentStoreResult" -> false,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }
  }

  "isAuthorisedForPaye" should {
    "return true when both Enrolment Store Proxy and EBS indicate that a relationship exists" in new Context {

      whenESPIsCheckedForPayeRelationship thenReturn (Future successful true)
      whenEBSIsCheckedForPayeRelationship thenReturn (Future successful true)

      await(
        authorisationService.isAuthorisedForPaye(
          agentCode,
          empRef,
          nonMtdAuthDetails)) shouldBe AccessResponse.Authorised

      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "paye",
        empRef,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> true,
          "ebsResult" -> true,
          "enrolmentStoreResult" -> true,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "return false when only Enrolment Store Proxy indicates a relationship exists" in new Context {

      whenESPIsCheckedForPayeRelationship thenReturn (Future successful true)
      whenEBSIsCheckedForPayeRelationship thenReturn (Future successful false)

      await(
        authorisationService.isAuthorisedForPaye(
          agentCode,
          empRef,
          nonMtdAuthDetails)) shouldBe AccessResponse.NoRelationship

      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "paye",
        empRef,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> false,
          "ebsResult" -> false,
          "enrolmentStoreResult" -> true,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "return false without calling DES if Enrolment Store Proxy Authorisation returns false (to reduce the load on DES)" in new Context {

      whenESPIsCheckedForPayeRelationship thenReturn (Future successful false)
      whenEBSIsCheckedForPayeRelationship thenAnswer failBecauseDesShouldNotBeCalled

      await(
        authorisationService.isAuthorisedForPaye(
          agentCode,
          empRef,
          nonMtdAuthDetails)) shouldBe AccessResponse.NoRelationship

      verify(mockAuditService).auditEvent(
        AgentAccessControlDecision,
        "agent access decision",
        agentCode,
        "paye",
        empRef,
        Seq(
          "credId" -> "ggId",
          "accessGranted" -> false,
          "ebsResult" -> "notChecked",
          "enrolmentStoreResult" -> false,
          "saAgentReference" -> saAgentRef,
          "affinityGroup" -> "Agent",
          "agentUserRole" -> User
        )
      )(hc, fakeRequest, ec)
    }

    "propagate any errors from Enrolment Store Proxy" in new Context {

      whenESPIsCheckedForPayeRelationship thenReturn (Future failed new BadRequestException(
        "bad request"))
      whenEBSIsCheckedForPayeRelationship thenReturn (Future successful true)

      intercept[BadRequestException] {
        await(
          authorisationService.isAuthorisedForPaye(agentCode,
                                                   empRef,
                                                   nonMtdAuthDetails))
      }
    }

    "propagate any errors from EBS" in new Context {

      whenESPIsCheckedForPayeRelationship thenReturn (Future successful true)
      whenEBSIsCheckedForPayeRelationship thenReturn (Future failed new BadRequestException(
        "bad request"))

      intercept[BadRequestException] {
        await(
          authorisationService.isAuthorisedForPaye(agentCode,
                                                   empRef,
                                                   nonMtdAuthDetails))
      }
    }
  }

  "isAuthorisedForAfi" should {

    "return Error when error encountered fetching agent record from DES" in new Context {
      whenAcaIsCheckedForSuspension() thenReturn Future.failed(
        UpstreamErrorResponse("boom", 503))

      await(authorisationService
        .isAuthorisedForAfi(agentCode, nino, afiAuthDetails)) should matchPattern {
        case AccessResponse.Error(_) =>
      }
    }

    "return AgentSuspended when agent is suspended" in new Context {
      whenAcaIsCheckedForSuspension() thenReturn Future.successful(
        SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      await(authorisationService
        .isAuthorisedForAfi(agentCode, nino, afiAuthDetails)) shouldBe AccessResponse.AgentSuspended
    }

    "return false when agent is not suspended and relationships do not exist" in new Context {
      whenAcaIsCheckedForSuspension() thenReturn Future.successful(
        SuspensionDetails.notSuspended)

      afiRelationshipConnectorIsCheckedForRelatioinships thenReturn Future(
        false)

      await(authorisationService
        .isAuthorisedForAfi(agentCode, nino, afiAuthDetails)) shouldBe AccessResponse.NoRelationship
    }

    "return true when agent is not suspended and relationships exist" in new Context {
      whenAcaIsCheckedForSuspension() thenReturn Future.successful(
        SuspensionDetails.notSuspended)

      afiRelationshipConnectorIsCheckedForRelatioinships thenReturn Future(true)

      await(authorisationService
        .isAuthorisedForAfi(agentCode, nino, afiAuthDetails)) shouldBe AccessResponse.Authorised
    }
  }

  private val failBecauseDesShouldNotBeCalled = new Answer[Future[Boolean]] {
    override def answer(invocation: InvocationOnMock): Future[Boolean] =
      fail("DES should not be called")
  }

  private abstract class Context {
    val mockDesAuthorisationService = mock[DesAuthorisationService]
    val mockESPAuthorisationService =
      mock[EnrolmentStoreProxyAuthorisationService]
    val mockAuditService = mock[AuditService]
    val mockAfiRelationshipConnector = mock[AfiRelationshipConnector]
    val mockMappingConnector = mock[MappingConnector]
    val servicesConfig = mock[ServicesConfig]
    val mockAcaConnector: AgentClientAuthorisationConnector =
      mock[AgentClientAuthorisationConnector]

    implicit val appConfig = new AppConfig(servicesConfig)

    val authorisationService = new AuthorisationService(
      mockDesAuthorisationService,
      mockESPAuthorisationService,
      mockAuditService,
      mockMappingConnector,
      mockAfiRelationshipConnector,
      mockAcaConnector
    )

    def whenESPIsCheckedForPayeRelationship() =
      when(
        mockESPAuthorisationService
          .isAuthorisedForPayeInEnrolmentStoreProxy("ggId", empRef))

    def whenESPIsCheckedForSaRelationship() =
      when(
        mockESPAuthorisationService
          .isAuthorisedForSaInEnrolmentStoreProxy("ggId", clientSaUtr))

    def whenEBSIsCheckedForPayeRelationship() =
      when(mockDesAuthorisationService.isAuthorisedInEbs(agentCode, empRef))

    def whenCesaIsCheckedForSaRelationship() =
      when(
        mockDesAuthorisationService
          .isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))

    def whenAcaIsCheckedForSuspension() =
      when(mockAcaConnector.getSuspensionDetails(arn))

    def afiRelationshipConnectorIsCheckedForRelatioinships =
      when(mockAfiRelationshipConnector.hasRelationship(arn.value, nino.value))

  }
}
