package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, CgtRef}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class CgtAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val arn = Arn("01234567890")
  val cgtRef = CgtRef("XMCGTP123456789")
  val providerId = "12345-credId"

  "GET /agent-access-control/cgt-auth/agent/:agentCode/client/:cgt" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)

      val status = authResponseFor(agentCode, cgtRef, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        val status = authResponseFor(agentCode, cgtRef, method).status

        status shouldBe 401
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

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)

      authResponseFor(agentCode, cgtRef, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, cgtRef, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__cgt-auth__agent__:__client__:-GET")
    }

    "handle suspended agents and return unauthorised" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, true, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, cgtRef, method).status shouldBe 401

      timerShouldExistsAndBeenUpdated("API-__cgt-auth__agent__:__client__:-GET")
    }
  }

  "POST /agent-access-control/cgt-auth/agent/:agentCode/client/:cgt" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)

      val status = authResponseFor(agentCode, cgtRef, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given()
          .agentAdmin(agentCode, providerId, None, None)
          .isAuthenticated()
          .givenAgentRecord(arn, false, "CGT")

        val status = authResponseFor(agentCode, cgtRef, method).status

        status shouldBe 401
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

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)

      authResponseFor(agentCode, cgtRef, method).status shouldBe 200
    }

    "record metrics for access control request" in {
      given()
        .agentAdmin(agentCode, providerId, None, Some(arn))
        .isAuthenticated()
        .givenAgentRecord(arn, false, "CGT")

      given()
        .mtdAgency(arn)
        .hasARelationshipWith(cgtRef)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, cgtRef, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-__cgt-auth__agent__:__client__:-POST")
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
