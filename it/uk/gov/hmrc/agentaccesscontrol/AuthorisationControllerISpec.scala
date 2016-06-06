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

import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeApplication
import uk.gov.hmrc.agentaccesscontrol.support.{DesStub, Resource}
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseISpec
  extends UnitSpec
  with OneServerPerSuite
  with StartAndStopWireMock
  with DesStub {

  private val configAuthControllerNeedsAuth = "controllers.uk.gov.hmrc.agentaccesscontrol.controllers.AuthorisationController.needsAuth"
  private val configDesHost = "microservice.services.des.host"
  private val configDesPort = "microservice.services.des.port"
  private val auditEnabled = "auditing.enabled"

  override implicit lazy val app: FakeApplication = FakeApplication(
    additionalConfiguration = Map(
      configAuthControllerNeedsAuth -> false,
      configDesHost -> wiremockHost,
      configDesPort -> wiremockPort,
      auditEnabled -> false
    )
  )
}

class AuthorisationControllerISpec extends BaseISpec {

  "/agent-access-control/sa-auth/agent/:agentCode/client/:saUtr" should {
    "respond with 401" when {
      "the agent is unknown" in {
        authResponseFor(unknownAgent, anUnlinkedClient).status shouldBe 401
      }

      "there is no relationship between the agent and the client" in {
        authResponseFor(theAgent, anUnlinkedClient).status shouldBe 401
      }

      "the client has authorised the agent only with 64-8, but not i64-8" in {
        authResponseFor(theAgent, aClientAuthorisedBy648).status shouldBe 401
      }

      "the client has authorised the agent only with i64-8, but not 64-8" in {
        authResponseFor(theAgent, aClientAuthorisedByI648).status shouldBe 401
      }

      "the client has authorised the agent with neither i64-8 nor 64-8" in {
        authResponseFor(theAgent, anUnauthorisedClient).status shouldBe 401
      }

      "there was an error in a downstream service" in {
        authResponseFor(agent500, anUnlinkedClient).status shouldBe 401
      }
    }

    "respond with 200" when {
      "the client has authorised the agent with both 64-8 and i64-8" in  {
        authResponseFor(theAgent, aFullyAuthorisedClient).status shouldBe 200
      }
    }
  }

  def authResponseFor(agentCode: AgentCode, clientSaUtr: SaUtr): HttpResponse =
    new Resource(s"/agent-access-control/sa-auth/agent/${agentCode.value}/client/${clientSaUtr.value}")(port).get()


}
