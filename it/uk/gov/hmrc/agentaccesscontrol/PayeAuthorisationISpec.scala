package uk.gov.hmrc.agentaccesscontrol

import play.utils.UriEncoding.encodePathSegment
import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportServerPerTest, Resource, WireMockWithOneServerPerTestISpec}
import uk.gov.hmrc.domain.{AgentCode, EmpRef}
import uk.gov.hmrc.http.HttpResponse

class PayeAuthorisationISpec extends WireMockWithOneServerPerTestISpec with MetricTestSupportServerPerTest {
  val agentCode = AgentCode("A11112222A")
  val empRef = EmpRef("123", "123456")
  val providerId = "12345-credId"

  "GET /agent-access-control/epaye-auth/agent/:agentCode/client/:empRef" should {
    val method = "GET"
    "return 200 when access is granted" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef, method).status

      status shouldBe 200
    }

    "return 401 when access is not granted" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsNotAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef, method).status

      status shouldBe 401
    }

    "return 502 if a downstream service fails" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andDesIsDown()

      val status = authResponseFor(agentCode, empRef, method).status

      status shouldBe 502
    }


    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      authResponseFor(agentCode, empRef, method).status shouldBe 200
    }
  }

  "POST /agent-access-control/epaye-auth/agent/:agentCode/client/:empRef" should {
    val method = "POST"
    "return 200 when access is granted" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef, method).status

      status shouldBe 200
    }

    "return 401 when access is not granted" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsNotAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef, method).status

      status shouldBe 401
    }

    "return 502 if a downstream service fails" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andDesIsDown()

      val status = authResponseFor(agentCode, empRef, method).status

      status shouldBe 502
    }

    "record metrics for inbound http call" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()
      givenCleanMetricRegistry()

      authResponseFor(agentCode, empRef, method).status shouldBe 200
      timerShouldExistsAndBeenUpdated("API-__epaye-auth__agent__:__client__:-POST")
    }

    "send an AccessControlDecision audit event" in {
      given()
        .agentAdmin(agentCode, providerId, None, None)
        .isAuthenticated()
        .andIsAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      authResponseFor(agentCode, empRef, method).status shouldBe 200
    }
  }

  def authResponseFor(agentCode: AgentCode, empRef: EmpRef, method: String): HttpResponse = {
    val resource = new Resource(
      s"/agent-access-control/epaye-auth/agent/${agentCode.value}/client/${encodePathSegment(empRef.value, "UTF-8")}")(
      port)
    method match {
      case "GET"  => resource.get()
      case "POST" => resource.post(body = """{"foo": "bar"}""")
    }
  }

}
