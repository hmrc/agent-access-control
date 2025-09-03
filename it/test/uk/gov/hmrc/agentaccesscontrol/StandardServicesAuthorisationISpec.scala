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

import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.NO_CONTENT
import play.api.test.Helpers.OK
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentAssuranceStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentClientRelationshipStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentPermissionsStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AuthStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.StandardServiceAuthorisationRequest
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._

class StandardServicesAuthorisationISpec
    extends ComponentSpecHelper
    with AuthStub
    with AgentAssuranceStub
    with AgentClientRelationshipStub
    with AgentPermissionsStub {

  private val NoRelationship = "NO_RELATIONSHIP"
  private val NoAssignment   = "NO_ASSIGNMENT"

  val standardServicesSupportedOnAsa: List[StandardServiceAuthorisationRequest] =
    List(Vat, ItsaMain, ItsaSupp, CgtPd, Trust, TrustNT, PPT, Pillar2)

  List("GET", "POST").foreach { requestMethod =>
    standardServicesSupportedOnAsa.foreach { serviceConfig =>
      s"$requestMethod ${serviceConfig.uri} with ${serviceConfig.taxIdentifierIdType}" when {
        "agent is not opted-in to access groups" should {
          "return 200 when the agent has been allocated the client's enrolment" in {
            stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
            stubAgentNotSuspended
            stubAgentClientRelationship(testArn, serviceConfig)(OK)
            stubAgentPermissionsOptInRecordExists(testArn)(NOT_FOUND)

            val result = {
              requestMethod match {
                case "GET"  => get(serviceConfig.uri)
                case "POST" => post(serviceConfig.uri)(Json.obj())
              }
            }

            result.status shouldBe OK
          }
          "return 401 when the agent has not been allocated the client's enrolment" in {
            stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
            stubAgentNotSuspended
            stubAgentPermissionsOptInRecordExists(testArn)(NOT_FOUND)
            stubAgentClientRelationship(testArn, serviceConfig)(NOT_FOUND)

            val result = {
              requestMethod match {
                case "GET"  => get(serviceConfig.uri)
                case "POST" => post(serviceConfig.uri)(Json.obj())
              }
            }

            result.status shouldBe 401
            result.body should include(NoRelationship)
          }
        }
        "agent has opted-in to access groups" should {
          "return 200 when the agent has been allocated the client's enrolment and a tax service group exists" in {
            stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
            stubAgentNotSuspended
            stubAgentPermissionsOptInRecordExists(testArn)(OK)
            stubAgentClientRelationship(testArn, serviceConfig)(OK)
            stubGetAgentPermissionTaxGroup(testArn, serviceConfig.taxGroup.service)(serviceConfig.taxGroup)

            val result = {
              requestMethod match {
                case "GET"  => get(serviceConfig.uri)
                case "POST" => post(serviceConfig.uri)(Json.obj())
              }
            }
            result.status shouldBe OK

          }

          "return 200 when the agent has been allocated the client's enrolment and assigned to the user" in {
            stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
            stubAgentNotSuspended
            stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
            stubAgentClientRelationship(testArn, serviceConfig)(OK)
            stubGetAgentPermissionTaxGroupNotFound(testArn, serviceConfig.taxGroup.service)
            stubAgentClientRelationshipAssigned(testArn, serviceConfig, testProviderId)(OK)

            val result = {
              requestMethod match {
                case "GET"  => get(serviceConfig.uri)
                case "POST" => post(serviceConfig.uri)(Json.obj())
              }
            }
            result.status shouldBe OK
          }

          "return 401 when the agent has been allocated the client's enrolment but it's not assigned to the user" in {
            stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
            stubAgentNotSuspended
            stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
            stubAgentClientRelationship(testArn, serviceConfig)(OK)
            stubGetAgentPermissionTaxGroupNotFound(testArn, serviceConfig.taxGroup.service)
            stubAgentClientRelationshipAssigned(testArn, serviceConfig, testProviderId)(NOT_FOUND)

            val result = {
              requestMethod match {
                case "GET"  => get(serviceConfig.uri)
                case "POST" => post(serviceConfig.uri)(Json.obj())
              }
            }
            result.status shouldBe UNAUTHORIZED
            result.body should include(NoAssignment)
          }

          "record metrics for access control request" in {
            stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
            stubAgentNotSuspended
            stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
            stubAgentClientRelationship(testArn, serviceConfig)(OK)
            stubGetAgentPermissionTaxGroup(testArn, serviceConfig.taxGroup.service)(serviceConfig.taxGroup)

            val result = {
              requestMethod match {
                case "GET"  => get(serviceConfig.uri)
                case "POST" => post(serviceConfig.uri)(Json.obj())
              }
            }

            result.status shouldBe 200
          }
        }
        "handle suspended for regime and return unauthorised" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentIsSuspended(serviceConfig.regime)

          val result = {
            requestMethod match {
              case "GET"  => get(serviceConfig.uri)
              case "POST" => post(serviceConfig.uri)(Json.obj())
            }
          }

          result.status shouldBe 401
          result.body should include(NoRelationship)
        }

        "handle suspended for AGSV regime and return unauthorised" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentIsSuspended("AGSV")

          val result = {
            requestMethod match {
              case "GET"  => get(serviceConfig.uri)
              case "POST" => post(serviceConfig.uri)(Json.obj())
            }
          }

          result.status shouldBe 401
          result.body should include(NoRelationship)
        }
      }
    }
  }
}
