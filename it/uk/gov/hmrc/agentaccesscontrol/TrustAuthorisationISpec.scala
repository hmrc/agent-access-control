package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class TrustAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val arn = Arn("01234567890")
  val utr = Utr("0123456789")
  val providerId = "12345-credId"

  "GET /agent-access-control/trust-auth/agent/:agentCode/client/:utr" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)

      val status = authResponseFor(agentCode, utr, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, false, "TRS")

        val status = authResponseFor(agentCode, utr, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "TRS")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(utr)

        val status = authResponseFor(agentCode, utr, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)

      authResponseFor(agentCode, utr, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, utr, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__trust-auth__agent__:__client__:-GET")
    }

    "handle suspended TRS regime and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, utr, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__trust-auth__agent__:__client__:-GET")
    }

    "handle suspended AGSV regime and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "AGSV")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, utr, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__trust-auth__agent__:__client__:-GET")
    }
  }

  "POST /agent-access-control/trust-auth/agent/:agentCode/client/:utr" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)

      val status = authResponseFor(agentCode, utr, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, false, "TRS")

        val status = authResponseFor(agentCode, utr, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "TRS")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(utr)

        val status = authResponseFor(agentCode, utr, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)

      authResponseFor(agentCode, utr, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "TRS")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(utr)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, utr, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__trust-auth__agent__:__client__:-POST")
    }
  }

  def authResponseFor(agentCode: AgentCode, utr: Utr, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/trust-auth/agent/${agentCode.value}/client/${utr.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
