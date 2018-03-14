package uk.gov.hmrc.agentaccesscontrol

import play.utils.UriEncoding.encodePathSegment
import uk.gov.hmrc.agentaccesscontrol.support.{ Resource, WireMockWithOneServerPerTestISpec }
import uk.gov.hmrc.domain.{ AgentCode, EmpRef }
import uk.gov.hmrc.http.HttpResponse

class ConfigPayeDisabledSpec extends WireMockWithOneServerPerTestISpec {

  val agentCode = AgentCode("A11112222A")
  val empRef = EmpRef("123", "123456")

  override protected def additionalConfiguration: Map[String, String] = Map("features.allowPayeAccess" -> "false")

  "PAYE delegated auth rule" should {

    "respond with 403 when PAYE is not enabled" in {
      givenLoggedInAgentIsPayeAuthorised
      authResponseFor(agentCode, empRef).status shouldBe 403
    }
  }

  def givenLoggedInAgentIsPayeAuthorised(): Unit = {
    given().agentAdmin(agentCode).isLoggedIn()
      .andHasNoIrSaAgentEnrolment()
      .andIsAssignedToClient(empRef)
      .andIsRelatedToPayeClientInDes(empRef)
      .andIsAuthorisedBy648()
  }

  def authResponseFor(agentCode: AgentCode, empRef: EmpRef): HttpResponse =
    new Resource(s"/agent-access-control/epaye-auth/agent/${agentCode.value}/client/${encodePathSegment(empRef.value, "UTF-8")}")(port).get()
}