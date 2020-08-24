package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Vrn}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class MtdVatAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val arn = Arn("01234567890")
  val vrn = Vrn("12345677890")
  val providerId = "12345-credId"

  "GET /agent-access-control/mtd-vat-auth/agent/:agentCode/client/:vrn" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)

      val status = authResponseFor(agentCode, vrn, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, false, "VATC")

        val status = authResponseFor(agentCode, vrn, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "VATC")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(vrn)

        val status = authResponseFor(agentCode, vrn, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)

      authResponseFor(agentCode, vrn, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, vrn, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__mtd-vat-auth__agent__:__client__:-GET")
    }

    "handle suspended agents and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)

      givenCleanMetricRegistry()

      authResponseFor(agentCode, vrn, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__mtd-vat-auth__agent__:__client__:-GET")
    }
  }

  "POST /agent-access-control/mtd-vat-auth/agent/:agentCode/client/:mtdItId" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)

      val status = authResponseFor(agentCode, vrn, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "VATC")

        val status = authResponseFor(agentCode, vrn, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "VATC")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(vrn)

        val status = authResponseFor(agentCode, vrn, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)

      authResponseFor(agentCode, vrn, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "VATC")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(vrn)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, vrn, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__mtd-vat-auth__agent__:__client__:-POST")
    }
  }

  def authResponseFor(agentCode: AgentCode, vrn: Vrn, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/mtd-vat-auth/agent/${agentCode.value}/client/${vrn.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
