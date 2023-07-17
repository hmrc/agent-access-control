package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}
import uk.gov.hmrc.http.HttpResponse

trait StandardAuthBehaviours extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {

  private val agentCode = AgentCode("ABCDEF123456")
  private val providerId = "12345-credId"
  private val arn = Arn("AARN0000002")

  def standardAuthBehaviour(authType: String, clientId: TaxIdentifier, regime: String): Unit = {
    def authResponseFor(agentCode: AgentCode, clientId: TaxIdentifier, method: String): HttpResponse = {
      val resource =
        new Resource(s"/agent-access-control/${authType}/agent/${agentCode.value}/client/${clientId.value}")(port)
      method match {
        case "GET"  => resource.get()
        case "POST" => resource.post(body = """{"foo": "bar"}""")
      }
    }

    s"GET /agent-access-control/${authType}/agent/:agentCode/client/:clientId" should {
      val method = "GET"
      "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, regime = regime)

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
            .givenAgentRecord(arn, suspended = false, regime = regime)

          val status = authResponseFor(agentCode, clientId, method).status

          status shouldBe 401
        }

        "there is no relationship between the agency and client" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .givenAgentRecord(arn, suspended = false, regime = regime)

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
          .givenAgentRecord(arn, suspended = false, regime = regime)

        given()
          .mtdAgency(arn)
          .hasARelationshipWith(clientId)

        authResponseFor(agentCode, clientId, method).status shouldBe 200
      }

      "record metrics for access control request" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, regime = regime)

        given()
          .mtdAgency(arn)
          .hasARelationshipWith(clientId)
        givenCleanMetricRegistry()

        authResponseFor(agentCode, clientId, method).status shouldBe 200

        timerShouldExistsAndBeenUpdated(s"API-__${authType}__agent__:__client__:-GET")
      }

      "handle suspended for regime and return unauthorised" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = true, regime = regime)

        given()
          .mtdAgency(arn)
          .hasARelationshipWith(clientId)
        givenCleanMetricRegistry()

        authResponseFor(agentCode, clientId, method).status shouldBe 401

        timerShouldExistsAndBeenUpdated(s"API-__${authType}__agent__:__client__:-GET")
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

        timerShouldExistsAndBeenUpdated(s"API-__${authType}__agent__:__client__:-GET")
      }
    }

    s"POST /agent-access-control/${authType}/agent/:agentCode/client/:clientId" should {
      val method = "POST"
      "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, regime = regime)

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
            .givenAgentRecord(arn, suspended = false, regime = regime)

          val status = authResponseFor(agentCode, clientId, method).status

          status shouldBe 401
        }

        "there is no relationship between the agency and client" in {
          given()
            .agentAdmin(agentCode, providerId, None, Some(arn))
            .isAuthenticated()
            .givenAgentRecord(arn, suspended = false, regime = regime)

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
          .givenAgentRecord(arn, suspended = false, regime = regime)

        given()
          .mtdAgency(arn)
          .hasARelationshipWith(clientId)

        authResponseFor(agentCode, clientId, method).status shouldBe 200
      }

      "record metrics for access control request" in {
        given()
          .agentAdmin(agentCode, providerId, None, Some(arn))
          .isAuthenticated()
          .givenAgentRecord(arn, suspended = false, regime = regime)

        given()
          .mtdAgency(arn)
          .hasARelationshipWith(clientId)
        givenCleanMetricRegistry()

        authResponseFor(agentCode, clientId, method).status shouldBe 200

        timerShouldExistsAndBeenUpdated(s"API-__${authType}__agent__:__client__:-POST")
      }
    }
  }
}
