/*
 * Copyright 2018 HM Revenue & Customs
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

import org.mockito.Matchers.{eq => eqs, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, MtdItAuthorisationService}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class AuthorisationControllerSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar {

  val auditService = mock[AuditService]
  val authorisationService = mock[AuthorisationService]
  val mtdItAuthorisationService = mock[MtdItAuthorisationService]
  def controller(enabled: Boolean = true) = new AuthorisationController(auditService, authorisationService, mtdItAuthorisationService, Configuration("features.allowPayeAccess" -> enabled))


  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditService, authorisationService)
  }


  private def anSaEndpoint(fakeRequest: FakeRequest[_<:AnyContent]) = {

    "return 401 if the AuthorisationService doesn't permit access" in {

      whenAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the AuthorisationService allows access" in {

      whenAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "pass request to AuthorisationService" in {

      whenAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      verify(authorisationService).isAuthorisedForSa(any[AgentCode], any[SaUtr])(any[ExecutionContext], any[HeaderCarrier], eqs(fakeRequest))

      status(response) shouldBe Status.OK
    }

    "propagate exception if the AuthorisationService fails" in {

      whenAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)))
    }

  }

  private def anMdtitEndpoint(fakeRequest: FakeRequest[_<:AnyContent]) = {
    "return 401 if the MtdAuthorisationService doesn't permit access" in {

      whenMtdAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForMtdIt(AgentCode(""), MtdItId("mtdItId"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }


    "return 200 if the MtdAuthorisationService allows access" in {

      whenMtdAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForMtdIt(AgentCode(""), MtdItId("mtdItId"))(fakeRequest)

      status(response) shouldBe Status.OK
    }


    "propagate exception if the MtdAuthorisationService fails" in {

      whenMtdAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForMtdIt(AgentCode(""), MtdItId("mtdItId"))(fakeRequest)))
    }
  }

  private def aPayeEndpoint(fakeRequest: FakeRequest[_<:AnyContent]) = {
    "return 200 when Paye is enabled" in {
      whenPayeAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForPaye(AgentCode(""), EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 200
    }

    "return 403 when Paye is disabled" in {
      whenPayeAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller(enabled = false).isAuthorisedForPaye(AgentCode(""), EmpRef("123", "123456"))(fakeRequest)

      status(response) shouldBe 403
    }
  }

  private def anAfiEndpoint(fakeRequest: FakeRequest[_<:AnyContent]) = {

    "return 200 if the AuthorisationService allows access" in {

      whenAfiAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller().isAuthorisedForAfi(AgentCode(""), Nino("AA123456A"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return 404 if the AuthorisationService does not allow access" in {

      whenAfiAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller().isAuthorisedForAfi(AgentCode(""), Nino("AA123456A"))(fakeRequest)

      status(response) shouldBe Status.NOT_FOUND
    }


    "propagate exception if the AuthorisationService fails" in {

      whenAfiAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller().isAuthorisedForAfi(AgentCode(""), Nino("AA123456A"))(fakeRequest)))
    }

  }


  "GET isAuthorisedForSa" should {
    behave like anSaEndpoint(FakeRequest("GET", "/agent-access-control/sa-auth/agent//client/utr"))
  }

  "POST isAuthorisedForSa" should {
    behave like anSaEndpoint(FakeRequest("POST", "/agent-access-control/sa-auth/agent//client/utr").withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForMtdIt" should {
    behave like anMdtitEndpoint(FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent//client/utr"))
  }

  "POST isAuthorisedForMtdIt" should {
    behave like anMdtitEndpoint(FakeRequest("POST", "/agent-access-control/mtd-it-auth/agent//client/utr").withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForPaye" should {
    behave like aPayeEndpoint(FakeRequest("GET", "/agent-access-control/epaye-auth/agent//client/utr"))
  }

  "POST isAuthorisedForPaye" should {
    behave like aPayeEndpoint(FakeRequest("POST", "/agent-access-control/epaye-auth/agent//client/utr").withJsonBody(Json.parse("{}")))
  }

  "GET isAuthorisedForAfi" should {
    behave like anAfiEndpoint(FakeRequest("GET", "/agent-access-control/afi-auth/agent//client/utr"))
  }

  "POST isAuthorisedForAfi" should {
    behave like anAfiEndpoint(FakeRequest("POST", "/agent-access-control/afi-auth/agent//client/utr").withJsonBody(Json.parse("{}")))
  }

  def whenAfiAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForAfi(any[AgentCode], any[Nino])(any[ExecutionContext], any[HeaderCarrier], any[Request[Any]]))

  def whenAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForSa(any[AgentCode], any[SaUtr])(any[ExecutionContext], any[HeaderCarrier], any[Request[Any]]))

  def whenMtdAuthorisationServiceIsCalled =
    when(mtdItAuthorisationService.authoriseForMtdIt(any[AgentCode], any[MtdItId])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))

  def whenPayeAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForPaye(any[AgentCode], any[EmpRef])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))
}
