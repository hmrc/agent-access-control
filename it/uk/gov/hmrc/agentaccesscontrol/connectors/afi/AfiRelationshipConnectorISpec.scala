package uk.gov.hmrc.agentaccesscontrol.connectors.afi

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.connectors.AfiRelationshipConnector
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentFiRelationshipStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AuthStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testAgentCode
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testArn
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testNino
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testProviderId
import uk.gov.hmrc.http.HeaderCarrier

class AfiRelationshipConnectorISpec
    extends ComponentSpecHelper
    with MetricTestSupport
    with AuthStub
    with AgentFiRelationshipStub {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector = app.injector.instanceOf[AfiRelationshipConnector]

  "hasRelationship" should {
    "return true when relationship exists" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentFiRelationship(testArn, testNino)(OK)

      await(connector.hasRelationship(testArn.value, testNino.value)) shouldBe true
    }

    "return false when relationship does not exist" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentFiRelationship(testArn, testNino)(NOT_FOUND)

      await(connector.hasRelationship(testArn.value, testNino.value)) shouldBe false
    }

    "throw exception when unexpected status code encountered" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentFiRelationship(testArn, testNino)(MULTIPLE_CHOICES)

      intercept[Exception] {
        await(connector.hasRelationship(testArn.value, testNino.value))
      }.getMessage shouldBe "Unsupported statusCode 300"
    }
  }
}
