package uk.gov.hmrc.agentaccesscontrol

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

import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.support.{Resource, WireMockWithOneServerPerSuiteISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.{AgentCode, Nino}
import uk.gov.hmrc.http.HttpResponse

class AfiAuthorisationISpec extends WireMockWithOneServerPerSuiteISpec {

  val agentCode = AgentCode("ABCDEF123456")
  val clientId = Nino("AE123456C")
  val arn = Arn("TARN0000001")

  private def anAfiEndpoint(method: String) ={
    "respond with 200" when {
      "the client has authorised the agent" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasHmrcAsAgentEnrolment(arn)
          .andHasRelationship(arn, clientId)

        authResponseFor(agentCode, clientId, method).status shouldBe 200

        DataStreamStub.verifyAuditRequestSent(
          AgentAccessControlDecision,
          Map("path" -> s"/agent-access-control/afi-auth/agent/$agentCode/client/$clientId"))
      }

    }

    "respond with 404" when {
      "the client has not authorised the agent" in {
        given()
          .agentAdmin(agentCode).isLoggedIn()
          .andHasHmrcAsAgentEnrolment(arn)
          .andHasNoRelationship(arn, clientId)

        authResponseFor(agentCode, clientId, method).status shouldBe 404

        DataStreamStub.verifyAuditRequestSent(
          AgentAccessControlDecision,
          Map("path" -> s"/agent-access-control/afi-auth/agent/$agentCode/client/$clientId"))
      }
    }
  }

  "GET /agent-access-control/afi-auth/agent/:agentCode/client/:nino" should {
    anAfiEndpoint("GET")
  }

  "POST /agent-access-control/afi-auth/agent/:agentCode/client/:nino" should {
    anAfiEndpoint("POST")
  }

  def authResponseFor(agentCode: AgentCode, nino: Nino, method: String): HttpResponse = {
    val resource = new Resource(s"/agent-access-control/afi-auth/agent/${agentCode.value}/client/${nino.value}")(port)
    method match {
      case "GET" => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
