package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.domain.TaxIdentifier

trait AgentClientAuthorisationStub extends WiremockMethods {
  def stubAgentClientAuthorisationSuspensionStatus(
      taxId: TaxIdentifier
  )(status: Int, isSuspended: Boolean, regime: String): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-authorisation/client/suspension-details/${taxId.value}"
    ).thenReturn(
      status = status,
      body = Json.obj("suspensionStatus" -> isSuspended, "regimes" -> Json.arr(regime)).toString
    )

}
