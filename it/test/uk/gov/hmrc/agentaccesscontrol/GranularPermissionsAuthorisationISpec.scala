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

import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.NO_CONTENT
import play.api.test.Helpers.OK
import uk.gov.hmrc.agentaccesscontrol.stubs._
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._

class GranularPermissionsAuthorisationISpec
    extends ComponentSpecHelper
    with MetricTestSupport
    with AuthStub
    with AgentClientAuthorisationStub
    with AgentClientRelationshipStub
    with AgentPermissionsStub
    with DataStreamStub {

  private val NoRelationship = "NO_RELATIONSHIP"
  private val NoAssignment   = "NO_ASSIGNMENT"

  private val uri                      = s"/cgt-auth/agent/${testAgentCode.value}/client/${testCgtRef.value}"
  private def trustUri(taxRef: String) = s"/trust-auth/agent/${testAgentCode.value}/client/$taxRef"
  private val cgtRegime                = "CGT"
  private val trustRegime              = "TRUST"

  // Using CGT as a sample, not checking this individually for all services
  s"GET $uri" should {
    "grant access" when {
      "agency and client have a relationship, the agency is opted-in to GP," +
        "user is not in a tax service group but the client is assigned directly to the user" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
          stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
          stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(OK)
          stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

          val result = get(uri)
          result.status shouldBe 200

          // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
          verifyCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(timesCalled = 1)
        }
      "agency and client have a relationship, agency is opted-in to GP," +
        "agent user IS in the relevant tax service group (even if client not assigned to agent user)" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
          stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
          stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(OK)
          stubGetAgentPermissionTaxGroup(testArn, testCgtEnrolmentKey)(OK, testCgtTaxGroup())
          stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

          val result = get(uri)
          result.status shouldBe 200

          // Check that we have NOT called agent-client-relationships to check for the user assignment - no need
          verifyCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(timesCalled = 0)
        }
      "agency and client have a relationship, agency is opted-in to GP," +
        "agent user IS in the relevant tax service group (special case for HMRC-TERS-ORG)" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
          stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
          stubTersAgentClientRelationship(testArn, testUtr)(OK)
          stubTersAgentClientRelationshipToUser(testArn, testUtr, testProviderId)(NOT_FOUND)
          stubGetAgentPermissionTaxGroup(testArn, "HMRC-TERS")(OK, testTrustTaxGroup)

          val result = get(trustUri(testUtr.value))
          result.status shouldBe 200
        }
      "agency and client have a relationship, agency is opted-in to GP," +
        "agent user IS in the relevant tax service group (special case for HMRC-TERSNT-ORG)" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
          stubTersntAgentClientRelationship(testArn, testUrn)(OK)
          stubTersntAgentClientRelationshipToUser(testArn, testUrn, testProviderId)(NOT_FOUND)
          stubGetAgentPermissionTaxGroup(testArn, "HMRC-TERS")(OK, testTrustTaxGroup)
          stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

          val result = get(trustUri(testUrn.value))
          result.status shouldBe 200
        }

      "the agency and client have a relationship and the agency is opted-out of granular permissions (even if the client is not assigned to the user)" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
        stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, "otherProviderId")(OK)
        stubGetAgentPermissionTaxGroup(testArn, testCgtEnrolmentKey)(OK, testCgtTaxGroup())
        stubAgentPermissionsOptInRecordExists(testArn)(NOT_FOUND)

        val result = get(uri)

        result.status shouldBe 200
      }
    }

    "not grant access" when {
      "agency and client have a relationship, agency is opted-in to GP but client is not assigned to the agent user" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
        stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, "otherProviderId")(OK)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

        val result = get(uri)

        result.status shouldBe 401
        result.body should include(NoAssignment)

        // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
        verifyCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(timesCalled = 1)
      }
      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(NOT_FOUND)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

        val result = get(uri)
        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
      "agency and client have a relationship, agency is opted-in to GP, agent user IS in a tax service group but for a different service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
        stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(NOT_FOUND)
        stubGetAgentPermissionTaxGroup(testArn, testMtdVatEnrolmentKey)(OK, testMtdVatTaxGroup)
        // tax service group is for vat, not cgt - shouldn't grant access
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

        val result = get(uri)
        result.status shouldBe 401
        result.body should include(NoAssignment)

        // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
        verifyCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(timesCalled = 1)
      }
      "agency and client have a relationship, agency is opted-in to GP," +
        "there is a tax service group for the right service but agent user is not part of it" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
          stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
          stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(NOT_FOUND)
          stubGetAgentPermissionTaxGroup(testArn, testCgtEnrolmentKey)(OK, testCgtTaxGroup("otherProviderId"))
          // the agent user is not part of this tax service group - shouldn't grant access
          stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

          val result = get(uri)
          result.status shouldBe 401
          result.body should include(NoAssignment)

          // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
          verifyCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(timesCalled = 1)
        }
      "agency and client have a relationship, agency is opted-in to GP," +
        "agent user is in the relevant tax service group but client is excluded from group" in {
          stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
          stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
          stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
          stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(NOT_FOUND)
          stubGetAgentPermissionTaxGroup(testArn, testCgtEnrolmentKey)(
            OK,
            testCgtTaxGroup(excludedClients = Set(testCgtClient))
          )
          stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

          val result = get(uri)
          result.status shouldBe 401
          result.body should include(NoAssignment)

          // Check that we have called agent-client-relationships to check for the user assignment (as the tax service group check should have failed)
          verifyCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(timesCalled = 1)
        }
      "GP enabled, agent user IS in the relevant tax service group but there is no agency-level relationship" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
        stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(NOT_FOUND)
        stubGetAgentPermissionTaxGroup(testArn, testCgtEnrolmentKey)(OK, testCgtTaxGroup())
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)

        val result = get(s"/cgt-auth/agent/${testAgentCode.value}/client/XMCGTP987654321")
        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }
  }

}
