package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, PptRef}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class PptAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("ABCDEF123456")
  val providerId = "12345-credId"
  val arn = Arn("AARN0000002")
  val clientId = PptRef("XHPPT0006633194")

  "GET /agent-access-control/ppt-auth/agent/:agentCode/client/:pptRef" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "PPT")

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
          .givenAgentRecord(arn, false, "PPT")

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "PPT")

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
        .givenAgentRecord(arn, false, "PPT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)

      authResponseFor(agentCode, clientId, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "PPT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__ppt-auth__agent__:__client__:-GET")
    }

    "handle suspended for PPT regime and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "PPT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__ppt-auth__agent__:__client__:-GET")
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

      timerShouldExistsAndBeenUpdated("API-__ppt-auth__agent__:__client__:-GET")
    }
  }

  "POST /agent-access-control/ppt-auth/agent/:agentCode/client/:pptRef" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "PPT")

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
          .givenAgentRecord(arn, false, "PPT")

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, false, "PPT")

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
        .givenAgentRecord(arn, false, "PPT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)

      authResponseFor(agentCode, clientId, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "PPT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__ppt-auth__agent__:__client__:-POST")
    }
  }

  def authResponseFor(agentCode: AgentCode, pptRef: PptRef, method: String): HttpResponse = {
    val resource =
      new Resource(s"/agent-access-control/ppt-auth/agent/${agentCode.value}/client/${pptRef.value}")(port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
