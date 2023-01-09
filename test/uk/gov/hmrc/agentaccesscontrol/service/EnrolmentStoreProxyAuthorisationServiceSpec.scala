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

import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.domain.{AgentCode, AgentUserId, EmpRef, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentaccesscontrol.support.UnitSpec

import scala.concurrent.Future

class EnrolmentStoreProxyAuthorisationServiceSpec
    extends UnitSpec
    with MockitoSugar
    with BeforeAndAfterEach {

  val esProxyConnector = mock[EnrolmentStoreProxyConnector]
  val service = new EnrolmentStoreProxyAuthorisationService(esProxyConnector)
  val agentCode1 = AgentCode("12AAAAA3A456")
  val agentCode2 = AgentCode("23BBBBB4B567")
  val saUtr = SaUtr("S123456789")
  val empRef = EmpRef("123", "43567890")
  implicit val hc = new HeaderCarrier()
  implicit val ec = concurrent.ExecutionContext.Implicits.global

  "isAuthorisedForSaInGovernmentGateway" should {
    behave like aGovernmentGatewayAssignmentCheck(
      when(esProxyConnector.getIRSADelegatedUserIdsFor(saUtr)(hc, ec)),
      (agentCode: AgentCode, credId: String) =>
        service.isAuthorisedForSaInEnrolmentStoreProxy(credId, saUtr)
    )
  }

  "isAuthorisedForPayeInGovernmentGateway" should {
    behave like aGovernmentGatewayAssignmentCheck(
      when(esProxyConnector.getIRPAYEDelegatedUserIdsFor(empRef)(hc, ec)),
      (agentCode: AgentCode, credId: String) =>
        service.isAuthorisedForPayeInEnrolmentStoreProxy(credId, empRef)
    )
  }

  private def aGovernmentGatewayAssignmentCheck(
      whenEnrolmentStoreProxyIsCalled: => OngoingStubbing[
        Future[Set[AgentUserId]]],
      assignmentCheck: (AgentCode, String) => Future[Boolean]) {
    "return true if the client is assigned to the agent credential" in {
      whenEnrolmentStoreProxyIsCalled thenReturn (Future successful Set(
        AgentUserId("000111333")))

      val result = await(assignmentCheck(agentCode1, "000111333"))

      result shouldBe true
    }

    "return true if there is more than one agent assigned to the client" in {
      whenEnrolmentStoreProxyIsCalled thenReturn (Future successful
        Set(AgentUserId("000111333"), AgentUserId("000111444")))

      val result = await(assignmentCheck(agentCode1, "000111444"))

      result shouldBe true
    }

    "return false if the client is not assigned to the agent credential" in {
      whenEnrolmentStoreProxyIsCalled thenReturn (Future successful Set(
        AgentUserId("000111444")))

      val result = await(assignmentCheck(agentCode1, "000111333"))

      result shouldBe false
    }

    "throw exception if enrolment store proxy fails" in {
      whenEnrolmentStoreProxyIsCalled thenThrow new RuntimeException()

      an[RuntimeException] should be thrownBy await(
        assignmentCheck(agentCode1, "NonMatchingCred"))
    }
  }

  override protected def beforeEach(): Unit =
    Mockito.reset(esProxyConnector)
}
