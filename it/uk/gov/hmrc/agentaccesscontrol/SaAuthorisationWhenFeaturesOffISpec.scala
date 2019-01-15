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
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HttpResponse

class SaAuthorisationWhenFeaturesOffISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {

  val agentCode = AgentCode("ABCDEF123456")
  val saAgentReference = SaAgentReference("ABC456")
  val clientUtr = SaUtr("123")
  val arn = Arn("AARN0000002")

  override def additionalConfiguration: Map[String, String] = Map(
    "features.allowAuthoriseMtdAgentForIRSA" -> "false"
  )

  "GET /agent-access-control/sa-auth/agent/:agentCode/client/:saUtr" should {
    val method = "GET"
    "respond with 401" when {
      "agent is not logged in" in {
        given()
          .agentAdmin(agentCode)
          .isNotLoggedIn()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent and client has no relation in DES" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andHasNoRelationInDesWith(clientUtr)

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "the client has authorised the agent only with 64-8, but not i64-8" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andIsAuthorisedByOnly648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "the client has authorised the agent only with i64-8, but not 64-8" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andIsAuthorisedByOnlyI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "the client has not authorised the agent" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .butIsNotAuthorised()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "the client is not assigned to the agent in Enrolment Store Proxy" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsNotAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred and has no mappings" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andIsAssignedToClient(clientUtr)
          .andHasArnWithEnrolment(arn)
          .givenNotFound404Mapping("sa", arn)

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred and searched with invalid or unsupported key" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .andIsAssignedToClient(clientUtr)
          .givenBadRequest400Mapping("sa", arn)

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred, has a mappings but no client delegated" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingSingular("sa", arn)
          .andHasSaAgentReference(saAgentReference)
          .andHasSaEnrolmentForAgent(saAgentReference)
          .andIsNotAssignedToClient(clientUtr)

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred, has multiple mappings but no client delegated" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingMultiple("sa", arn)
          .andHasSaEnrolmentForAgent(SaAgentReference("SA6012"), "88741987654329", "88741987654322")
          .andHasSaEnrolmentForAgent(saAgentReference, "98741987654329", "98741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("A1709A"), "78741987654329", "78741987654322")
          .andHasSaAgentReference(saAgentReference)
          .andIsNotAssignedToClient(clientUtr)

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred, has multiple mappings, gg has delegate relatioship but ETMP has no relationship" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingMultiple("sa", arn)
          .andHasSaEnrolmentForAgent(saAgentReference, "98741987654329", "98741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("SA6012"), "88741987654329", "88741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("A1709A"), "78741987654329", "78741987654322")
          .andHasSaAgentReference(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .butIsNotAuthorised()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }
    }

    "respond with 502 (bad gateway)" when {
      "DES is down" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andDesIsDown()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 502
      }

      "Enrolment Store Proxy is down" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()
          .andEnrolmentStoreProxyIsDown(clientUtr)

        authResponseFor(agentCode, clientUtr, method).status shouldBe 502
      }
    }

    "respond with 200" when {

      "agent is enrolled to IR-SA-AGENT but the enrolment is not activated and the the client has authorised the agent with both 64-8 and i64-8" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 200
      }

      "agent uses an MTD cred, has mapping and SaAgentRef has enrolment with only one groupId" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingSingular("sa", arn)
          .andHasSaAgentReference(saAgentReference)
          .andHasSaEnrolmentForAgent(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred, has mapping and SaAgentRef has enrolment with multiple groupId" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingSingular("sa", arn)
          .andHasSaAgentReference(saAgentReference)
          .andHasSaEnrolmentForAgent(saAgentReference)
          .andIsAssignedToClient(clientUtr, "823982983983")
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred, has multiple mappings and SaAgentRef has enrolment with multiple groupId" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingMultiple("sa", arn)
          .andHasSaEnrolmentForAgent(saAgentReference, "98741987654329", "98741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("SA6012"), "88741987654329", "88741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("A1709A"), "78741987654329", "78741987654322")
          .andHasSaAgentReference(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "agent uses an MTD cred, has multiple mappings and SaAgentRef has enrolment with groupId" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasArnWithEnrolment(arn)
          .givenSaMappingMultiple("sa", arn)
          .andHasSaEnrolmentForAgent(saAgentReference, "98741987654329", "98741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("SA6012"), "88741987654329", "88741987654322")
          .andHasSaEnrolmentForAgent(SaAgentReference("A1709A"), "78741987654329", "78741987654322")
          .andHasSaAgentReference(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 401
      }

      "the client has authorised the agent with both 64-8 and i64-8" in {
        given()
          .agentAdmin(agentCode)
          .isLoggedIn()
          .andHasSaAgentReferenceWithEnrolment(saAgentReference)
          .andIsAssignedToClient(clientUtr)
          .andIsRelatedToSaClientInDes(clientUtr)
          .andAuthorisedByBoth648AndI648()

        authResponseFor(agentCode, clientUtr, method).status shouldBe 200
      }
    }

    "record metrics for inbound http call" in {
      given()
        .agentAdmin(agentCode)
        .isLoggedIn()
        .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
        .andIsAssignedToClient(clientUtr)
        .andIsRelatedToSaClientInDes(clientUtr)
        .andAuthorisedByBoth648AndI648()
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientUtr, method).status shouldBe 200
      timerShouldExistsAndBeenUpdated("API-__sa-auth__agent__:__client__:-GET")
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode)
        .isLoggedIn()
        .andHasSaAgentReferenceWithPendingEnrolment(saAgentReference)
        .andIsAssignedToClient(clientUtr)
        .andIsRelatedToSaClientInDes(clientUtr)
        .andAuthorisedByBoth648AndI648()

      authResponseFor(agentCode, clientUtr, method).status shouldBe 200

      DataStreamStub.verifyAuditRequestSent(
        AgentAccessControlDecision,
        Map("path" -> s"/agent-access-control/sa-auth/agent/$agentCode/client/$clientUtr"))
    }
  }

  def authResponseFor(agentCode: AgentCode, clientSaUtr: SaUtr, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/sa-auth/agent/${agentCode.value}/client/${clientSaUtr.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }

}