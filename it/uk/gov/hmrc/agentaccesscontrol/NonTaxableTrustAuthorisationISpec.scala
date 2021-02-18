package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Urn}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class NonTaxableTrustAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {

  val agentCode: AgentCode = AgentCode("A11112222A")
  val arn: Arn = Arn("01234567890")
  val urn: Urn = Urn("urn12345677890")
  val providerId: String = "12345-credId"

  "GET /agent-access-control/non-taxable-trust-auth/agent/:agentCode/client/:urn" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = false, "TRS")
      given().mtdAgency(arn).hasARelationshipWith(urn)

      val status = authResponseFor(agentCode, urn, method).status
      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, "TRS")

        val status = authResponseFor(agentCode, urn, method).status
        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, regime = "TRS")
        given().mtdAgency(arn).hasNoRelationshipWith(urn)

        val status = authResponseFor(agentCode, urn, method).status
        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = false, "TRS")

      given().mtdAgency(arn).hasARelationshipWith(urn)

      authResponseFor(agentCode, urn, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = false, "TRS")
      given().mtdAgency(arn).hasARelationshipWith(urn)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, urn, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__non-taxable-trust-auth__agent__:__client__:-GET")
    }

    "handle suspended agents and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = true, "TRS")
      given().mtdAgency(arn).hasARelationshipWith(urn)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, urn, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__non-taxable-trust-auth__agent__:__client__:-GET")
    }
  }

  "POST /agent-access-control/non-taxable-trust-auth/agent/:agentCode/client/:urn" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = false, "TRS")
      given().mtdAgency(arn).hasARelationshipWith(urn)

      val status = authResponseFor(agentCode, urn, method).status
      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, "TRS")

        val status = authResponseFor(agentCode, urn, method).status
        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, "TRS")

        given().mtdAgency(arn).hasNoRelationshipWith(urn)

        val status = authResponseFor(agentCode, urn, method).status
        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = false, "TRS")
      given().mtdAgency(arn).hasARelationshipWith(urn)

      authResponseFor(agentCode, urn, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, suspended = false, "TRS")
      given().mtdAgency(arn).hasARelationshipWith(urn)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, urn, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__non-taxable-trust-auth__agent__:__client__:-POST")
    }
  }

  def authResponseFor(agentCode: AgentCode, urn: Urn, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/non-taxable-trust-auth/agent/${agentCode.value}/client/${urn.value}")(port)
    method match {
      case "GET" => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
