package uk.gov.hmrc.agentaccesscontrol

import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HttpResponse

class MtdItAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val arn = Arn("01234567890")
  val clientId = MtdItId("12345677890")

  "GET /agent-access-control/mtd-it-auth/agent/:agentCode/client/:mtdItId" should {
    val method = "GET"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(clientId)

      val status = authResponseFor(agentCode, clientId, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given().agentAdmin(agentCode).isLoggedIn()
          .andHasNoHmrcAsAgentEnrolment()

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given().agentAdmin(agentCode).isLoggedIn()
          .andHasHmrcAsAgentEnrolment(arn)
        given().mtdAgency(arn)
          .hasNoRelationshipWith(clientId)

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(clientId)

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      DataStreamStub.verifyAuditRequestSent(
        AgentAccessControlDecision,
        Map("path" -> s"/agent-access-control/mtd-it-auth/agent/$agentCode/client/${clientId.value}"))
    }

    "record metrics for access control request" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-Agent-MTD-IT-Access-Control-GET")
    }
  }

  "POST /agent-access-control/mtd-it-auth/agent/:agentCode/client/:mtdItId" should {
    val method = "POST"
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(clientId)

      val status = authResponseFor(agentCode, clientId, method).status

      status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        given().agentAdmin(agentCode).isLoggedIn()
          .andHasNoHmrcAsAgentEnrolment()

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }

      "there is no relationship between the agency and client" in {
        given().agentAdmin(agentCode).isLoggedIn()
          .andHasHmrcAsAgentEnrolment(arn)
        given().mtdAgency(arn)
          .hasNoRelationshipWith(clientId)

        val status = authResponseFor(agentCode, clientId, method).status

        status shouldBe 401
      }
    }

    "send an AccessControlDecision audit event" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(clientId)

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      DataStreamStub.verifyAuditRequestSent(
        AgentAccessControlDecision,
        Map("path" -> s"/agent-access-control/mtd-it-auth/agent/$agentCode/client/${clientId.value}"))
    }

    "record metrics for access control request" ignore {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasHmrcAsAgentEnrolment(arn)
      given().mtdAgency(arn)
        .hasARelationshipWith(clientId)
      givenCleanMetricRegistry()

      authResponseFor(agentCode, clientId, method).status shouldBe 200

      timerShouldExistsAndBeenUpdated("API-Agent-MTD-IT-Access-Control-GET")
    }
  }

  def authResponseFor(agentCode: AgentCode, mtdItId: MtdItId, method: String): HttpResponse = {
    val resource = new Resource(s"/agent-access-control/mtd-it-auth/agent/${agentCode.value}/client/${mtdItId.value}")(port)
    method match {
      case "GET" => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }
}
