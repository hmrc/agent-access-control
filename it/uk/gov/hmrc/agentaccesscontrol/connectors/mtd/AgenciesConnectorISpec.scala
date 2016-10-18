package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import java.net.URL

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.model.Arn
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.http.HeaderCarrier

class AgenciesConnectorISpec extends WireMockWithOneAppPerSuiteISpec {

  val agentCode = AgentCode("A1111A")
  val arn = Arn("B1111B")
  implicit val hc = HeaderCarrier()

  "fetchAgencyRecord" should {
    "audit the call" in new Context {
      given().mtdAgency(agentCode, arn)
        .isAnMtdAgency()

      await(connector.fetchAgencyRecord(agentCode))

      anOutboundCallShouldBeAudited(agentCode)
    }
  }

  abstract class Context extends MockAuditingContext {
    def connector = new AgenciesConnector(new URL(wiremockBaseUrl), wsHttp)

    def anOutboundCallShouldBeAudited(agentCode: AgentCode) = {
      val event = capturedEvent()

      event.auditType shouldBe "OutboundCall"

      event.request.tags("path") shouldBe s"$wiremockBaseUrl/agencies-fake/agencies/agentcode/${agentCode.value}"
    }
  }
}
