package uk.gov.hmrc.agentaccesscontrol

import com.kenshoo.play.metrics.MetricsRegistry
import play.utils.UriEncoding.encodePathSegment
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.support.{WireMockWithOneServerPerSuiteISpec, Resource}
import uk.gov.hmrc.domain.{AgentCode, EmpRef}
import uk.gov.hmrc.play.http.HttpResponse

class PayeAuthorisationISpec extends WireMockWithOneServerPerSuiteISpec {
  val agentCode = AgentCode("A11112222A")
  val empRef = EmpRef("123", "123456")

  "/agent-access-control/paye-auth/agent/:agentCode/client/:empRef" should {
    "return 200 when access is granted" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsAllocatedAndAssignedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef).status

      status shouldBe 200
    }

    "return 401 when access is not granted" in {
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsNotAllocatedToClient(empRef)
        .andIsRelatedToPayeClientInDes(empRef)
        .andIsAuthorisedBy648()

      val status = authResponseFor(agentCode, empRef).status

      status shouldBe 401
    }

    "return 502 if a downstream service fails" in{
      given().agentAdmin(agentCode).isLoggedIn()
        .andHasNoIrSaAgentEnrolment()
        .andIsAllocatedAndAssignedToClient(empRef)
        .andDesIsDown()

      val status = authResponseFor(agentCode, empRef).status

      status shouldBe 502
    }
  }

  def authResponseFor(agentCode: AgentCode, empRef: EmpRef): HttpResponse =
    new Resource(s"/agent-access-control/paye-auth/agent/${agentCode.value}/client/${encodePathSegment(empRef.value, "UTF-8")}")(port).get()
}
