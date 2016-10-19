/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.mvc.Http.Status
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.model.MtdSaClientId
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, MtdAuthorisationService}
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar {

  val fakeRequest = FakeRequest("GET", "/agent-access-control/sa-auth/agent//client/utr")

  val auditService = mock[AuditService]
  val authorisationService = mock[AuthorisationService]
  val mtdAuthorisationService = mock[MtdAuthorisationService]
  val controller = new AuthorisationController(auditService, authorisationService, mtdAuthorisationService)


  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditService, authorisationService)
  }

  "isAuthorisedForSa" should {

    "return 401 if the AuthorisationService doesn't permit access" in {
      whenAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller.isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }

    "return 200 if the AuthorisationService allows access" in {
      whenAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller.isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "pass request to AuthorisationService" in {
      whenAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller.isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)

      verify(authorisationService).isAuthorised(any[AgentCode], any[SaUtr])(any[ExecutionContext], any[HeaderCarrier], eqs(fakeRequest))

      status(response) shouldBe Status.OK
    }

    "propagate exception if the AuthorisationService fails" in {

      whenAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller.isAuthorisedForSa(AgentCode(""), SaUtr("utr"))(fakeRequest)))
    }
  }

  "isAuthorisedForMtdSa" should {

    "return 401 if the MtdAuthorisationService doesn't permit access" in {

      whenMtdAuthorisationServiceIsCalled thenReturn(Future successful false)

      val response = controller.isAuthorisedForMtdSa(AgentCode(""), MtdSaClientId("utr"))(fakeRequest)

      status (response) shouldBe Status.UNAUTHORIZED
    }


    "return 200 if the MtdAuthorisationService allows access" in {

      whenMtdAuthorisationServiceIsCalled thenReturn(Future successful true)

      val response = controller.isAuthorisedForMtdSa(AgentCode(""), MtdSaClientId("utr"))(fakeRequest)

      status(response) shouldBe Status.OK
    }


    "propagate exception if the MtdAuthorisationService fails" in {

      whenMtdAuthorisationServiceIsCalled thenReturn(Future failed new IllegalStateException("some error"))

      an[IllegalStateException] shouldBe thrownBy(status(controller.isAuthorisedForMtdSa(AgentCode(""), MtdSaClientId("utr"))(fakeRequest)))
    }
  }
  def whenAuthorisationServiceIsCalled =
    when(authorisationService.isAuthorised(any[AgentCode], any[SaUtr])(any[ExecutionContext], any[HeaderCarrier], any[Request[Any]]))

  def whenMtdAuthorisationServiceIsCalled =
    when(mtdAuthorisationService.authoriseForSa(any[AgentCode], any[MtdSaClientId])(any[ExecutionContext], any[HeaderCarrier], any[Request[_]]))
}
