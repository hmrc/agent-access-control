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

import uk.gov.hmrc.agentaccesscontrol.support.{Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.{AgentCode, Nino}
import uk.gov.hmrc.http.HttpResponse

class AfiAuthorisationISpec extends WireMockWithOneServerPerTestISpec {

  val agentCode = AgentCode("ABCDEF123456")
  val clientId = Nino("AE123456C")
  val arn = Arn("TARN0000001")
  val providerId = "12345-credId"

  val afiAuthUri = s"/agent-access-control/afi-auth/agent/${agentCode.value}/client/${clientId.value}"
  val GET = "GET"
  val POST = "POST"
  val regime = "AGSV"

  s"Calling $afiAuthUri" when {

    s"http method is $GET" should {

      "respond with 200" when {
        "the client has authorised the agent and agent is not suspended" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .andHasRelationship(arn, clientId)
            .givenSuspensionStatus(arn, suspended = false, regime)

          authResponseFor(GET).status shouldBe 200
        }
      }

      "respond with 401" when {
        "the client has authorised the agent but agent is suspended" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .andHasRelationship(arn, clientId)
            .givenSuspensionStatus(arn, suspended = true, regime)

          authResponseFor(GET).status shouldBe 401
        }
      }

      "respond with 401" when {
        "the client has not authorised the agent and agent is not suspended" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .andHasNoRelationship(arn, clientId)
            .givenSuspensionStatus(arn, suspended = false, regime)

          authResponseFor(GET).status shouldBe 401
        }
      }
    }

    s"http method is $POST" should {

      "respond with 200" when {
        "the client has authorised the agent and agent is not suspended" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .andHasRelationship(arn, clientId)
            .givenSuspensionStatus(arn, suspended = false, regime)

          authResponseFor(POST).status shouldBe 200
        }
      }

      "respond with 401" when {
        "the client has authorised the agent but agent is suspended" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .andHasRelationship(arn, clientId)
            .givenSuspensionStatus(arn, suspended = true, regime)

          authResponseFor(POST).status shouldBe 401
        }
      }

      "respond with 401" when {
        "the client has not authorised the agent and agent is not suspended" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .andHasNoRelationship(arn, clientId)
            .givenSuspensionStatus(arn, suspended = false, regime)

          authResponseFor(POST).status shouldBe 401
        }
      }
    }
  }

  def authResponseFor(method: String): HttpResponse = {
    val resource = new Resource(afiAuthUri)(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
