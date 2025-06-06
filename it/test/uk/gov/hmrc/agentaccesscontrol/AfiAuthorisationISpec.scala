/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.stubs._
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._

class AfiAuthorisationISpec
    extends ComponentSpecHelper
    with AuthStub
    with AgentFiRelationshipStub
    with AgentAssuranceStub {

  private val uri: String    = s"/afi-auth/agent/${testAgentCode.value}/client/${testNino.value}"
  private val regime: String = "AGSV"

  s"Calling $uri" when {
    s"http method is $GET" should {
      "respond with 200" when {
        "the client has authorised the agent and agent is not suspended" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentFiRelationship(testArn, testNino)(OK)
          stubAgentNotSuspended

          val result = get(uri)

          result.status shouldBe OK
        }
      }

      "respond with 401" when {
        "the client has authorised the agent but agent is suspended" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentFiRelationship(testArn, testNino)(OK)
          stubAgentIsSuspended(regime)

          val result = get(uri)

          result.status shouldBe UNAUTHORIZED
        }

        "the client has not authorised the agent and agent is not suspended" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentFiRelationship(testArn, testNino)(NOT_FOUND)
          stubAgentNotSuspended

          val result = get(uri)

          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    s"http method is $POST" should {
      "respond with 200" when {
        "the client has authorised the agent and agent is not suspended" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentFiRelationship(testArn, testNino)(OK)
          stubAgentNotSuspended

          val result = post(uri)(Json.obj())

          result.status shouldBe OK
        }
      }

      "respond with 401" when {
        "the client has authorised the agent but agent is suspended" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentFiRelationship(testArn, testNino)(OK)
          stubAgentIsSuspended(regime)

          val result = post(uri)(Json.obj())

          result.status shouldBe UNAUTHORIZED
        }

        "the client has not authorised the agent and agent is not suspended" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentFiRelationship(testArn, testNino)(NOT_FOUND)
          stubAgentNotSuspended

          val result = post(uri)(Json.obj())

          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }
}
