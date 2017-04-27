/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, MtdItAuthorisationService}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar {

  val fakeRequest = FakeRequest("GET", "/agent-access-control/sa-auth/agent//client/utr")
  val payeFakeRequest = FakeRequest("GET", "/agent-access-control/epaye-auth/agent/A11112222A/client/123%2F123456 ")

  val auditService = mock[AuditService]
  val authorisationService = mock[AuthorisationService]
  val mtdItAuthorisationService = mock[MtdItAuthorisationService]
  def controller(enabled: Boolean = true) = new AuthorisationController(auditService, authorisationService, mtdItAuthorisationService, Configuration("features.allowPayeAccess" -> enabled))


  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditService, authorisationService)
  }

  "isAuthorisedForSa" should {

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

  "isAuthorisedForMtdIt" should {

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

    "isAuthorisedForPaye" should {

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
  }
  def whenAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForSa(any[AgentCode], any[SaUtr])(any[ExecutionContext], any[HeaderCarrier], any[Request[Any]]))

  def whenMtdAuthorisationServiceIsCalled =
    when(mtdItAuthorisationService.authoriseForMtdIt(any[AgentCode], any[MtdItId])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))

  def whenPayeAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorisedForPaye(any[AgentCode], any[EmpRef])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))
}
