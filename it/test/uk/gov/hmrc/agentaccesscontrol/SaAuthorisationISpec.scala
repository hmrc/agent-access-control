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

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentMappingStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AuthStub
import uk.gov.hmrc.agentaccesscontrol.stubs.DesStub
import uk.gov.hmrc.agentaccesscontrol.stubs.EnrolmentStoreProxyStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._
import uk.gov.hmrc.domain.SaAgentReference

class SaAuthorisationISpec
    extends ComponentSpecHelper
    with AuthStub
    with EnrolmentStoreProxyStub
    with DesStub
    with AgentMappingStub {

  val url = s"/sa-auth/agent/${testAgentCode.value}/client/${testSaUtr.value}"

  "GET /agent-access-control/sa-auth/agent/:agentCode/client/:saUtr" should {
    "respond with UNAUTHORIZED" when {
      "agent is not logged in" in {
        stubAuthUserIsNotAuthenticated()

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "agent and client has no relation in DES" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(NOT_FOUND, Json.obj())

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "the client has authorised the agent only with 64-8, but not i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = false)
        )

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "the client has authorised the agent only with i64-8, but not 64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "the client has not authorised the agent" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "the client is not assigned to the agent in Enrolment Store Proxy" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred and has no mappings" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubAgentMappingSa(testArn)(NOT_FOUND, Json.obj())

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred and searched with invalid or unsupported key" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubAgentMappingSa(testArn)(BAD_REQUEST, Json.obj())

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred, has a mappings but no client delegated" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred, has multiple mappings but no client delegated" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("SA6012"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "88741987654329", "88741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "98741987654329", "98741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("A1709A"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "78741987654329", "78741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred, has multiple mappings, gg has delegate relationship but ETMP has no relationship" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("SA6012"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "88741987654329", "88741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "98741987654329", "98741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("A1709A"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "78741987654329", "78741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        val result = get(url)
        result.status shouldBe UNAUTHORIZED
      }
    }

    "respond with BAD_GATEWAY" when {
      "DES is down" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(INTERNAL_SERVER_ERROR, Json.obj())

        val result = get(url)
        result.status shouldBe BAD_GATEWAY
      }

      "Enrolment Store Proxy is down" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(INTERNAL_SERVER_ERROR, Json.obj())

        val result = get(url)
        result.status shouldBe BAD_GATEWAY
      }
    }

    "respond with OK" when {

      "agent is enrolled to IR-SA-AGENT but the enrolment is not activated and the the client has authorised the agent with both 64-8 and i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe OK
      }

      "agent uses an MTD cred, has mapping and SaAgentRef has enrolment with only one groupId" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe OK
      }

      "agent uses an MTD cred, has mapping and SaAgentRef has enrolment with multiple groupId" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(
          OK,
          successfulResponseDelegated(Seq(testProviderId, "823982983983"))
        )
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe OK
      }

      "agent uses an MTD cred, has multiple mappings and SaAgentRef has enrolment with multiple groupId" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "98741987654329", "98741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("SA6012"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "88741987654329", "88741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("A1709A"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "78741987654329", "78741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe OK
      }

      "the client has authorised the agent with both 64-8 and i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe OK
      }

      "record metrics for inbound http call" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = get(url)
        result.status shouldBe OK
      }
    }
  }

  "POST /agent-access-control/sa-auth/agent/:agentCode/client/:saUtr" should {
    "respond with UNAUTHORIZED" when {
      "agent is not logged in" in {
        stubAuthUserIsNotAuthenticated()

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "agent and client has no relation in DES" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(NOT_FOUND, Json.obj())

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "the client has authorised the agent only with 64-8, but not i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = false)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "the client has authorised the agent only with i64-8, but not 64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "the client has not authorised the agent" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "the client is not assigned to the agent in Enrolment Store Proxy" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred and has no mappings" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubAgentMappingSa(testArn)(NOT_FOUND, Json.obj())

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred and searched with invalid or unsupported key" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubAgentMappingSa(testArn)(BAD_REQUEST, Json.obj())

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred, has a mappings but no client delegated" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred, has multiple mappings but no client delegated" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("SA6012"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "88741987654329", "88741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "98741987654329", "98741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("A1709A"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "78741987654329", "78741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }

      "agent uses an MTD cred, has multiple mappings, gg has delegate relationship but ETMP has no relationship" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("SA6012"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "88741987654329", "88741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "98741987654329", "98741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("A1709A"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "78741987654329", "78741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe UNAUTHORIZED
      }
    }

    "respond with BAD_GATEWAY" when {
      "DES is down" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(INTERNAL_SERVER_ERROR, Json.obj())

        val result = post(url)(Json.obj())
        result.status shouldBe BAD_GATEWAY
      }

      "Enrolment Store Proxy is down" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(INTERNAL_SERVER_ERROR, Json.obj())

        val result = post(url)(Json.obj())
        result.status shouldBe BAD_GATEWAY
      }
    }

    "respond with OK" when {

      "agent is enrolled to IR-SA-AGENT but the enrolment is not activated and the the client has authorised the agent with both 64-8 and i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe OK
      }

      "agent uses an MTD cred, has mapping and SaAgentRef has enrolment with only one groupId" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe OK
      }

      "agent uses an MTD cred, has mapping and SaAgentRef has enrolment with multiple groupId" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulSingularResponse(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(
          OK,
          successfulResponseDelegated(Seq(testProviderId, "823982983983"))
        )
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe OK
      }

      "agent uses an MTD cred, has multiple mappings and SaAgentRef has enrolment with multiple groupId" in {
        stubAuth(
          OK,
          successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), Some(testSaAgentReference))
        )
        stubAgentMappingSa(testArn)(OK, successfulMultipleResponses(testArn, testSaAgentReference))
        stubQueryUsersAssignedEnrolmentsPrincipalSa(testSaAgentReference)(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "98741987654329", "98741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("SA6012"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "88741987654329", "88741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsPrincipalSa(SaAgentReference("A1709A"))(
          OK,
          successfulResponsePrincipal(Seq(testProviderId, "78741987654329", "78741987654322"))
        )
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe OK
      }

      "the client has authorised the agent with both 64-8 and i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe OK
      }

      "record metrics for inbound http call" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, None, Some(testSaAgentReference)))
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        val result = post(url)(Json.obj())
        result.status shouldBe OK
      }
    }
  }

}
