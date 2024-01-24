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

package uk.gov.hmrc.agentaccesscontrol.services

import play.api.test.Helpers.await
import uk.gov.hmrc.agentaccesscontrol.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyAuthorisationServiceSpec extends UnitSpec {

  trait Setup {
    protected val mockEnrolmentStoreProxyConnector
      : EnrolmentStoreProxyConnector =
      mock[EnrolmentStoreProxyConnector]

    object TestService
        extends EnrolmentStoreProxyAuthorisationService(
          mockEnrolmentStoreProxyConnector
        )
  }

  private val saUtr = SaUtr("S123456789")
  private val empRef = EmpRef("123", "43567890")
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val ec: ExecutionContext =
    concurrent.ExecutionContext.Implicits.global

  "isAuthorisedForSaInEnrolmentStoreProxy" should {
    "return true if the client is assigned to the agent credential" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSADelegatedUserIdsFor(saUtr) returns
        Future.successful(Set(AgentUserId("000111333")))

      val result = await(
        TestService.isAuthorisedForSaInEnrolmentStoreProxy("000111333", saUtr))

      result mustBe true
    }

    "return true if there is more than one agent assigned to the client" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSADelegatedUserIdsFor(saUtr) returns
        Future.successful(
          Set(AgentUserId("000111333"), AgentUserId("000111444")))

      val result = await(
        TestService.isAuthorisedForSaInEnrolmentStoreProxy("000111444", saUtr))

      result mustBe true
    }

    "return false if the client is not assigned to the agent credential" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSADelegatedUserIdsFor(saUtr) returns
        Future.successful(Set(AgentUserId("000111444")))

      val result = await(
        TestService.isAuthorisedForSaInEnrolmentStoreProxy("000111333", saUtr))

      result mustBe false
    }

    "throw exception if enrolment store proxy fails" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSADelegatedUserIdsFor(saUtr) returns
        Future.failed(new RuntimeException())

      an[RuntimeException] should be thrownBy await(
        TestService.isAuthorisedForSaInEnrolmentStoreProxy("NonMatchingCred",
                                                           saUtr)
      )
    }
  }

  "isAuthorisedForPayeInEnrolmentStoreProxy" should {
    "return true if the client is assigned to the agent credential" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRPAYEDelegatedUserIdsFor(empRef) returns
        Future.successful(Set(AgentUserId("000111333")))

      val result = await(
        TestService.isAuthorisedForPayeInEnrolmentStoreProxy("000111333",
                                                             empRef))

      result mustBe true
    }

    "return true if there is more than one agent assigned to the client" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRPAYEDelegatedUserIdsFor(empRef) returns
        Future.successful(
          Set(AgentUserId("000111333"), AgentUserId("000111444")))

      val result = await(
        TestService.isAuthorisedForPayeInEnrolmentStoreProxy("000111444",
                                                             empRef))

      result mustBe true
    }

    "return false if the client is not assigned to the agent credential" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRPAYEDelegatedUserIdsFor(empRef) returns
        Future.successful(Set(AgentUserId("000111444")))

      val result = await(
        TestService.isAuthorisedForPayeInEnrolmentStoreProxy("000111333",
                                                             empRef))

      result mustBe false
    }

    "throw exception if enrolment store proxy fails" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRPAYEDelegatedUserIdsFor(empRef) returns
        Future.failed(new RuntimeException())

      an[RuntimeException] should be thrownBy await(
        TestService.isAuthorisedForPayeInEnrolmentStoreProxy("NonMatchingCred",
                                                             empRef))
    }
  }

  "getAgentUserIdsFor" should {
    "Return multiple Agent User Ids if they were found" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.successful(
          Set(AgentUserId("000111444"), AgentUserId("444111000")))

      val result =
        await(TestService.getAgentUserIdsFor(SaAgentReference("ref1")))

      result mustBe Set(AgentUserId("000111444"), AgentUserId("444111000"))
    }
    "Return a single Agent User Id if only 1 was found" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.successful(Set(AgentUserId("000111444")))

      val result =
        await(TestService.getAgentUserIdsFor(SaAgentReference("ref1")))

      result mustBe Set(AgentUserId("000111444"))
    }
    "Return an empty Array if ESP found no user ids" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.successful(Set.empty)

      val result =
        await(TestService.getAgentUserIdsFor(SaAgentReference("ref1")))

      result mustBe Set.empty
    }
    "Throw an exception if the ESP call fails" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.failed(new RuntimeException())

      an[RuntimeException] should be thrownBy await(
        TestService.getAgentUserIdsFor(SaAgentReference("ref1")))
    }
  }

  "getAgentUserIdsFor - sequence" should {
    "Populate both lists if both references found results" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.successful(
          Set(AgentUserId("000111444"), AgentUserId("444111000")))

      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref2")) returns
        Future.successful(
          Set(AgentUserId("999111444"), AgentUserId("444111999")))

      val result = await(
        TestService.getAgentUserIdsFor(
          Seq(SaAgentReference("ref1"), SaAgentReference("ref2"))))

      result mustBe Seq(
        (SaAgentReference("ref1"),
         Set(AgentUserId("000111444"), AgentUserId("444111000"))),
        (SaAgentReference("ref2"),
         Set(AgentUserId("999111444"), AgentUserId("444111999")))
      )
    }

    "Still return a list if the other didn't find results" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.successful(
          Set(AgentUserId("000111444"), AgentUserId("444111000")))

      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref2")) returns
        Future.successful(Set.empty)

      val result = await(
        TestService.getAgentUserIdsFor(
          Seq(SaAgentReference("ref1"), SaAgentReference("ref2"))))

      result mustBe Seq((SaAgentReference("ref1"),
                         Set(AgentUserId("000111444"),
                             AgentUserId("444111000"))),
                        (SaAgentReference("ref2"), Set.empty))
    }

    "Throw an exception if the ESP call fails" in new Setup {
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1")) returns
        Future.successful(Set(AgentUserId("000111444")))
      mockEnrolmentStoreProxyConnector.getIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref2")) returns
        Future.failed(new RuntimeException())

      an[RuntimeException] should be thrownBy await(
        TestService.getAgentUserIdsFor(
          Seq(SaAgentReference("ref1"), SaAgentReference("ref2"))))
    }
  }

}
