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

import java.util.Base64

import uk.gov.hmrc.agentaccesscontrol.support.{Resource, WireMockWithOneServerPerSuiteISpec}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.HttpResponse

class WhitelistISpec extends WireMockWithOneServerPerSuiteISpec {

  val agentCode = AgentCode("ABCDEF123456")
  val saAgentReference = SaAgentReference("ABC456")
  val clientUtr = SaUtr("123")
  val saService = "IR-SA"

  override protected def additionalConfiguration: Map[String, String] = Map(
    "microservice.whitelist.enabled" -> "true",
    "microservice.whitelist.ips" -> Base64.getEncoder().encodeToString("192.168.1.2,192.168.1.3".getBytes))


  "SA delegated auth rule" should {
    "respond with NOT_IMPLEMENTED if whitelist is enabled and there is no IP address in header" in {
      givenLoggedInAgentIsAuthorised()

      authResponseFor(agentCode, clientUtr, None).status shouldBe 501
    }

    "respond with FORBIDDEN if whitelist is enabled and there is an IP address in header that is not on the list" in {
      givenLoggedInAgentIsAuthorised()

      authResponseFor(agentCode, clientUtr, Some("192.168.1.1")).status shouldBe 403
    }

    "be enabled if whitelist is enabled and there is an IP address in header that is on the list" in {
      givenLoggedInAgentIsAuthorised()

      authResponseFor(agentCode, clientUtr, Some("192.168.1.2")).status shouldBe 200
      authResponseFor(agentCode, clientUtr, Some("192.168.1.3")).status shouldBe 200
    }
  }

  "ping" should {
    "be available regardless of IP address in the header" in {
      new Resource("/ping/ping")(port).get().status shouldBe 200
    }
  }

  "admin details" should {
    "be available regardless of IP address in the header" in {
      new Resource("/admin/details")(port).get().status shouldBe 200
    }
  }

  "metrics" should {
    "be available regardless of IP address in the header" in {
      new Resource("/admin/metrics")(port).get().status shouldBe 200
    }
  }


  def givenLoggedInAgentIsAuthorised(): Unit = {
    given()
      .agentAdmin(agentCode).isLoggedIn()
      .andHasSaAgentReferenceWithEnrolment(saAgentReference)
      .andIsAllocatedAndAssignedToClient(clientUtr, saService)
      .andIsRelatedToClientInDes(clientUtr).andAuthorisedByBoth648AndI648()
  }

  def authResponseFor(agentCode: AgentCode, clientSaUtr: SaUtr, trueClientIp: Option[String]): HttpResponse =
    new Resource(s"/agent-access-control/sa-auth/agent/${agentCode.value}/client/${clientSaUtr.value}")(port).get(trueClientIp)

}
