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

import uk.gov.hmrc.agentaccesscontrol.helpers.UnitTest
import uk.gov.hmrc.agentaccesscontrol.mocks.connectors.MockESPConnector
import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyAuthorisationServiceSpec
    extends UnitTest
    with MockESPConnector {

  private val service = new EnrolmentStoreProxyAuthorisationService(
    mockESPConnector)
  private val saUtr = SaUtr("S123456789")
  private val empRef = EmpRef("123", "43567890")
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val ec: ExecutionContext =
    concurrent.ExecutionContext.Implicits.global

  "isAuthorisedForSaInEnrolmentStoreProxy" should {
    "return true if the client is assigned to the agent credential" in {
      mockGetIRSADelegatedUserIdsFor(
        saUtr,
        Future.successful(Set(AgentUserId("000111333"))))

      val result = await(
        service.isAuthorisedForSaInEnrolmentStoreProxy("000111333", saUtr))

      result shouldBe true
    }

    "return true if there is more than one agent assigned to the client" in {
      mockGetIRSADelegatedUserIdsFor(
        saUtr,
        Future.successful(
          Set(AgentUserId("000111333"), AgentUserId("000111444"))))

      val result = await(
        service.isAuthorisedForSaInEnrolmentStoreProxy("000111444", saUtr))

      result shouldBe true
    }

    "return false if the client is not assigned to the agent credential" in {
      mockGetIRSADelegatedUserIdsFor(
        saUtr,
        Future.successful(Set(AgentUserId("000111444"))))

      val result = await(
        service.isAuthorisedForSaInEnrolmentStoreProxy("000111333", saUtr))

      result shouldBe false
    }

    "throw exception if enrolment store proxy fails" in {
      mockGetIRSADelegatedUserIdsFor(saUtr,
                                     Future.failed(new RuntimeException()))

      an[RuntimeException] should be thrownBy await(
        service.isAuthorisedForSaInEnrolmentStoreProxy("NonMatchingCred", saUtr)
      )
    }
  }

  "isAuthorisedForPayeInEnrolmentStoreProxy" should {
    "return true if the client is assigned to the agent credential" in {
      mockGetIRPAYEDelegatedUserIdsFor(
        empRef,
        Future.successful(Set(AgentUserId("000111333"))))

      val result = await(
        service.isAuthorisedForPayeInEnrolmentStoreProxy("000111333", empRef))

      result shouldBe true
    }

    "return true if there is more than one agent assigned to the client" in {
      mockGetIRPAYEDelegatedUserIdsFor(
        empRef,
        Future.successful(
          Set(AgentUserId("000111333"), AgentUserId("000111444"))))

      val result = await(
        service.isAuthorisedForPayeInEnrolmentStoreProxy("000111444", empRef))

      result shouldBe true
    }

    "return false if the client is not assigned to the agent credential" in {
      mockGetIRPAYEDelegatedUserIdsFor(
        empRef,
        Future.successful(Set(AgentUserId("000111444"))))

      val result = await(
        service.isAuthorisedForPayeInEnrolmentStoreProxy("000111333", empRef))

      result shouldBe false
    }

    "throw exception if enrolment store proxy fails" in {
      mockGetIRPAYEDelegatedUserIdsFor(empRef,
                                       Future.failed(new RuntimeException()))

      an[RuntimeException] should be thrownBy await(
        service.isAuthorisedForPayeInEnrolmentStoreProxy("NonMatchingCred",
                                                         empRef))
    }
  }

  "getAgentUserIdsFor" should {
    "Return multiple Agent User Ids if they were found" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1"),
        Future.successful(
          Set(AgentUserId("000111444"), AgentUserId("444111000"))))

      val result = await(service.getAgentUserIdsFor(SaAgentReference("ref1")))

      result shouldBe Set(AgentUserId("000111444"), AgentUserId("444111000"))
    }
    "Return a single Agent User Id if only 1 was found" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1"),
        Future.successful(Set(AgentUserId("000111444"))))

      val result = await(service.getAgentUserIdsFor(SaAgentReference("ref1")))

      result shouldBe Set(AgentUserId("000111444"))
    }
    "Return an empty Array if ESP found no user ids" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(SaAgentReference("ref1"),
                                          Future.successful(Set.empty))

      val result = await(service.getAgentUserIdsFor(SaAgentReference("ref1")))

      result shouldBe Set.empty
    }
    "Throw an exception if the ESP call fails" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(SaAgentReference("ref1"),
                                          Future.failed(new RuntimeException()))

      an[RuntimeException] should be thrownBy await(
        service.getAgentUserIdsFor(SaAgentReference("ref1")))
    }
  }

  "getAgentUserIdsFor - sequence" should {
    "Populate both lists if both references found results" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1"),
        Future.successful(
          Set(AgentUserId("000111444"), AgentUserId("444111000"))))

      mockGetIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref2"),
        Future.successful(
          Set(AgentUserId("999111444"), AgentUserId("444111999"))))

      val result = await(
        service.getAgentUserIdsFor(
          Seq(SaAgentReference("ref1"), SaAgentReference("ref2"))))

      result shouldBe Seq(
        (SaAgentReference("ref1"),
         Set(AgentUserId("000111444"), AgentUserId("444111000"))),
        (SaAgentReference("ref2"),
         Set(AgentUserId("999111444"), AgentUserId("444111999")))
      )
    }

    "Still return a list if the other didn't find results" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1"),
        Future.successful(
          Set(AgentUserId("000111444"), AgentUserId("444111000"))))

      mockGetIRSAAGENTPrincipalUserIdsFor(SaAgentReference("ref2"),
                                          Future.successful(Set.empty))

      val result = await(
        service.getAgentUserIdsFor(
          Seq(SaAgentReference("ref1"), SaAgentReference("ref2"))))

      result shouldBe Seq((SaAgentReference("ref1"),
                           Set(AgentUserId("000111444"),
                               AgentUserId("444111000"))),
                          (SaAgentReference("ref2"), Set.empty))
    }

    "Throw an exception if the ESP call fails" in {
      mockGetIRSAAGENTPrincipalUserIdsFor(
        SaAgentReference("ref1"),
        Future.successful(Set(AgentUserId("000111444"))))
      mockGetIRSAAGENTPrincipalUserIdsFor(SaAgentReference("ref2"),
                                          Future.failed(new RuntimeException()))

      an[RuntimeException] should be thrownBy await(
        service.getAgentUserIdsFor(
          Seq(SaAgentReference("ref1"), SaAgentReference("ref2"))))
    }
  }

}
