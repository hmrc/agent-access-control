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

import uk.gov.hmrc.agentaccesscontrol.support.{BaseISpec, Resource}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HttpResponse

class AuthorisationControllerISpec extends BaseISpec {

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

      "agent is not enrolled to SA" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andIsNotEnrolledForSA()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "agent and client has no relation" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReference(saAgentReference)
          .andHasNoRelationInDesWith(clientUtr)

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client has authorised the agent only with 64-8, but not i64-8" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReference(saAgentReference)
          .andIsRelatedToClient(clientUtr).andIsAuthorisedByOnly648()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client has authorised the agent only with i64-8, but not 64-8" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReference(saAgentReference)
          .andIsRelatedToClient(clientUtr).andIsAuthorisedByOnlyI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }

      "the client has not authorised the agent" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReference(saAgentReference)
          .andIsRelatedToClient(clientUtr).butIsNotAuthorised()

        authResponseFor(agentCode, clientUtr).status shouldBe 401
      }
    }

    "respond with 200" when {
      "the client has authorised the agent with both 64-8 and i64-8" in  {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasSaAgentReference(saAgentReference)
          .andIsRelatedToClient(clientUtr).andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr).status shouldBe 200
      }
    }

  }
  
  def authResponseFor(agentCode: AgentCode, clientSaUtr: SaUtr): HttpResponse =
    new Resource(s"/agent-access-control/sa-auth/agent/${agentCode.value}/client/${clientSaUtr.value}")(port).get()


}
