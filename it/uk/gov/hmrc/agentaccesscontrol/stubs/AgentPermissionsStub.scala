package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.TaxGroup

trait AgentPermissionsStub extends WiremockMethods {

  def stubAgentPermissionsOptInRecordExists(arn: Arn)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-permissions/arn/${arn.value}/optin-record-exists"
    ).thenReturn(status)

  def stubGetAgentPermissionTaxGroup(arn: Arn, taxService: String)(status: Int, taxGroup: TaxGroup): StubMapping =
    when(
      method = GET,
      uri = s"/agent-permissions/arn/${arn.value}/tax-group/$taxService"
    ).thenReturn(status, taxGroup)

}
