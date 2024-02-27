package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino

trait AgentFiRelationshipStub extends WiremockMethods {

  def stubAgentFiRelationship(arn: Arn, clientId: Nino)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}"
    ).thenReturn(status)

}
