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

package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.support.{Resource, WireMockWithOneServerPerSuiteISpec}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HttpResponse

class SaAuthorisationISpec extends WireMockWithOneServerPerSuiteISpec {

  val agentCode = AgentCode("ABCDEF123456")
  val saAgentReference = SaAgentReference("ABC456")
  val clientUtr = SaUtr("123")

  "/agent-access-control/sa-auth/agent/:agentCode/client/:saUtr" should {
    "respond with 401" when {
      "agent is not logged in" in {
        given()
          .agentAdmin(agentCode).isNotLoggedIn()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "agent and client has no relation in DES" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andHasNoRelationInDesWith(clientUtr)

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client has authorised the agent only with 64-8, but not i64-8" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).andIsAuthorisedByOnly648()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client has authorised the agent only with i64-8, but not 64-8" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).andIsAuthorisedByOnlyI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client has not authorised the agent" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).butIsNotAuthorised()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client is not allocated to the agency in GG" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsNotAllocatedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client is allocated to the agency but not assigned to the agent in GG" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAllocatedButNotAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }
    }

    "respond with 502 (bad gateway)" when {
      "DES is down" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andDesIsDown()

        authResponseFor(agentCode, clientUtr).status shouldBe 502
      }

      "GG is down" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
          .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()
          .andGGIsDown(clientUtr)

        authResponseFor(agentCode, clientUtr).status shouldBe 502
      }
    }

    "respond with 200" when {

      "agent is enrolled to IR-SA-AGENT but the enrolment is not activated and the the client has authorised the agent with both 64-8 and i64-8" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 200
      }

      "the client has authorised the agent with both 64-8 and i64-8" in  {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAllocatedAndAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 200
      }
    }

    "record metrics for inbound http call" in {
      val metricsRegistry = app.injector.instanceOf[MicroserviceMonitoringFilter].kenshooRegistry
      given()
        .agentAdmin(agentCode).isLoggedIn()
        .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
        .andIsAllocatedAndAssignedToClient(clientUtr)
        .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()

      authResponseFor(agentCode, clientUtr).status shouldBe 200
      metricsRegistry.getTimers().get("Timer-API-Agent-SA-Access-Control-GET").getCount should be >= 1L
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode).isLoggedIn()
        .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
        .andIsAllocatedAndAssignedToClient(clientUtr)
        .andIsRelatedToSaClientInDes(clientUtr).andAuthorisedByBoth648AndI648()

      authResponseFor(agentCode, clientUtr).status shouldBe 200

      DataStreamStub.verifyAuditRequestSent(
        AgentAccessControlDecision,
        Map("path" -> s"/agent-access-control/sa-auth/agent/$agentCode/client/$clientUtr"))
    }
  }
  
  def authResponseFor(agentCode: AgentCode, clientSaUtr: SaUtr): HttpResponse =
    new Resource(s"/agent-access-control/sa-auth/agent/${agentCode.value}/client/${clientSaUtr.value}")(port).get()


}
