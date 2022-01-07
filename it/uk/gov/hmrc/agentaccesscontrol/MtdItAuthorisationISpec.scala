package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class MtdItAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("ABCDEF123456")
  val providerId = "12345-credId"
  val arn = Arn("AARN0000002")
  val clientId = MtdItId("12345677890")

  "GET /agent-access-control/mtd-it-auth/agent/:agentCode/client/:mtdItId" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)

      val status = authResponseFor(agentCode, clientId, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "ITSA")

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "ITSA")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(clientId)

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)

      authResponseFor(agentCode, clientId, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__mtd-it-auth__agent__:__client__:-GET")
    }

    "handle suspended for ITSA regime and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__mtd-it-auth__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "AGSV")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__mtd-it-auth__agent__:__client__:-GET")
    }
  }

  "POST /agent-access-control/mtd-it-auth/agent/:agentCode/client/:mtdItId" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)

      val status = authResponseFor(agentCode, clientId, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, false, "ITSA")

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "ITSA")

        given()
          .mtdAgency(arn)
          .hasNoRelationshipWith(clientId)

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)

      authResponseFor(agentCode, clientId, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "ITSA")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__mtd-it-auth__agent__:__client__:-POST")
    }
  }

  def authResponseFor(agentCode: AgentCode, mtdItId: MtdItId, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/mtd-it-auth/agent/${agentCode.value}/client/${mtdItId.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
