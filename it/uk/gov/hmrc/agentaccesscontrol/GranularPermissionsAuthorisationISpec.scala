package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, CgtRef}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse
import com.github.tomakehurst.wiremock.client.WireMock

class GranularPermissionsAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val arn = Arn("01234567890")
  val cgtRef = CgtRef("XMCGTP123456789")
  val providerId = "12345-credId"

  // Using CGT as a sample, not checking this individually for all services
  "GET /agent-access-control/cgt-auth/agent/:agentCode/client/:cgt" should {
    val method = "GET"
    "grant access" when {
      "the agency and client have a relationship, the agency is opted-in to granular permissions and the client is assigned to the user" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasAssignedRelationshipToAgentUser(cgtRef, providerId)

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 200

        // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}")).withQueryParam("userId", WireMock.equalTo(providerId)))
      }
      "the agency and client have a relationship and the agency is opted-out of granular permissions (even if the client is not assigned to the user)" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedOutOfGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasAssignedRelationshipToAgentUser(cgtRef, "someOtherUserId")

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 200
      }
    }

    "not grant access" when {
      "the agency and client have a relationship, the agency is opted-in to granular permissions but the client is not assigned to the user" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasAssignedRelationshipToAgentUser(cgtRef, "someOtherUserId")

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 401

        // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}")).withQueryParam("userId", WireMock.equalTo(providerId)))
      }
      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(cgtRef)

        val status = authResponseFor(agentCode, cgtRef, method).status

        status shouldBe 401
      }
    }
  }

  def authResponseFor(agentCode: AgentCode, cgtRef: CgtRef, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/cgt-auth/agent/${agentCode.value}/client/${cgtRef.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
