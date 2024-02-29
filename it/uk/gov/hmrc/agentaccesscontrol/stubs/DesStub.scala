package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.SaUtr

trait DesStub extends WiremockMethods {

  def successfulDesSaResponse(auth_64_8: Boolean, auth_i64_8: Boolean): JsObject =
    Json.obj(
      "Auth_64-8"  -> auth_64_8,
      "Auth_i64-8" -> auth_i64_8
    )

  def successfulDesPayeResponse(auth_64_8: Boolean): JsObject = Json.obj("Auth_64-8" -> auth_64_8)

  def stubDesSaAgentClientRelationship(
      saAgentReference: SaAgentReference,
      saUtr: SaUtr
  )(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/sa/agents/${saAgentReference.value}/client/${saUtr.value}",
      headers = Map("Authorization" -> "Bearer secret", "Environment" -> "test")
    ).thenReturn(status, body)

  def stubDesPayeAgentClientRelationship(agentCode: AgentCode, empRef: EmpRef)(
      status: Int,
      body: JsValue
  ): StubMapping =
    when(
      method = GET,
      uri =
        s"/agents/regime/PAYE/agent/${agentCode.value}/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}",
      headers = Map("Authorization" -> "Bearer secret", "Environment" -> "test")
    ).thenReturn(status, body)

}
