package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, CgtRef, Urn, Utr}
import uk.gov.hmrc.agents.accessgroups.Client
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
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
      "agency and client have a relationship, the agency is opted-in to GP, user is not in a tax service group but the client is assigned directly to the user" in {
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
      "agency and client have a relationship, agency is opted-in to GP, agent user IS in the relevant tax service group (even if client not assigned to agent user)" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasNoAssignedRelationshipToAgentUser(cgtRef, providerId)
          .userIsInTaxServiceGroup("HMRC-CGT-PD", providerId)

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 200

        // Check that we have NOT called agent-client-relationships to check for the user assignment - no need
        WireMock.verify(0, WireMock.getRequestedFor(WireMock.urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}")).withQueryParam("userId", WireMock.equalTo(providerId)))
      }
      "agency and client have a relationship, agency is opted-in to GP, agent user IS in the relevant tax service group (special case for HMRC-TERS-ORG)" in {
        val utr = Utr("5066836985")
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "TRUST")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(utr)
          .hasNoAssignedRelationshipToAgentUser(utr, providerId)
          .userIsInTaxServiceGroup("HMRC-TERS", providerId) // tax service groups use the nonstandard key "HMRC-TERS" to mean either type of trust

        authResponseFor(agentCode, utr, method).status shouldBe 200
      }
      "agency and client have a relationship, agency is opted-in to GP, agent user IS in the relevant tax service group (special case for HMRC-TERSNT-ORG)" in {
        val urn = Urn("XATRUST06683698")
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "TRUST")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(urn)
          .hasNoAssignedRelationshipToAgentUser(urn, providerId)
          .userIsInTaxServiceGroup("HMRC-TERS", providerId) // tax service groups use the nonstandard key "HMRC-TERS" to mean either type of trust

        authResponseFor(agentCode, urn, method).status shouldBe 200
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
      "agency and client have a relationship, agency is opted-in to GP but client is not assigned to the agent user" in {
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
      "agency and client have a relationship, agency is opted-in to GP, agent user IS in a tax service group but for a different service" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasNoAssignedRelationshipToAgentUser(cgtRef, providerId)
          .userIsInTaxServiceGroup("HMRC-MTD-VAT", providerId) // tax service group is for vat, not cgt - shouldn't grant access

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 401

        // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}")).withQueryParam("userId", WireMock.equalTo(providerId)))
      }
      "agency and client have a relationship, agency is opted-in to GP, there is a tax service group for the right service but agent user is not part of it" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasNoAssignedRelationshipToAgentUser(cgtRef, providerId)
          .userIsInTaxServiceGroup("HMRC-CGT-PD", "someOtherUserId") // the agent user is not part of this tax service group - shouldn't grant access

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 401

        // Check that we have called agent-client-relationships specifying for which agent user to check the relationship
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}")).withQueryParam("userId", WireMock.equalTo(providerId)))
      }
      "agency and client have a relationship, agency is opted-in to GP, agent user is in the relevant tax service group but client is excluded from group" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasNoAssignedRelationshipToAgentUser(cgtRef, providerId)
          .userIsInTaxServiceGroup("HMRC-CGT-PD", providerId, excludedClients = Set(Client(s"HMRC-CGT-PD~CGTPDRef~${cgtRef.value}", "Zoe Client")))

        val status = authResponseFor(agentCode, cgtRef, method).status
        status shouldBe 401

        // Check that we have called agent-client-relationships to check for the user assignment (as the tax service group check should have failed)
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}")).withQueryParam("userId", WireMock.equalTo(providerId)))
      }
      "GP enabled, agent user IS in the relevant tax service group but there is no agency-level relationship" in {
        val aStrangersCgtRef = CgtRef("XMCGTP987654321")
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        given()
          .mtdAgency(arn)
          .isOptedInToGranularPermissions
          .hasARelationshipWith(cgtRef)
          .hasNoAssignedRelationshipToAgentUser(cgtRef, providerId)
          .userIsInTaxServiceGroup("HMRC-CGT-PD", providerId)

        val status = authResponseFor(agentCode, aStrangersCgtRef, method).status
        status shouldBe 401
      }
    }
  }

  def authResponseFor(agentCode: AgentCode, taxRef: TaxIdentifier, method: String): HttpResponse = {
    val taxUrlPart = taxRef match {
      case _: CgtRef => "cgt-auth"
      case _: Utr => "trust-auth"
      case _: Urn => "trust-auth"
    }
    val resource =
      new Resource(s"/agent-access-control/$taxUrlPart/agent/${agentCode.value}/client/${taxRef.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
