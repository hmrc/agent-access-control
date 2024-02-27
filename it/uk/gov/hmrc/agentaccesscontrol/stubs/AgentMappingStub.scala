package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference

trait AgentMappingStub extends WiremockMethods {

  def successfulSingularResponse(arn: Arn, saAgentReference: SaAgentReference): JsObject =
    Json.obj(
      "mappings" -> Json.arr(
        Json.obj("arn" -> arn.value, "identifier" -> saAgentReference.value)
      )
    )

  def successfulMultipleResponses(arn: Arn, saAgentReference: SaAgentReference): JsObject =
    Json.obj(
      "mappings" -> Json.arr(
        Json.obj(
          "arn"        -> arn.value,
          "identifier" -> saAgentReference.value
        ),
        Json.obj(
          "arn"        -> arn.value,
          "identifier" -> "A1709A"
        ),
        Json.obj(
          "arn"        -> arn.value,
          "identifier" -> "SA6012"
        )
      )
    )

  def stubAgentMappingSa(arn: Arn)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/agent-mapping/mappings/key/sa/arn/${arn.value}"
    ).thenReturn(status, body)

}
