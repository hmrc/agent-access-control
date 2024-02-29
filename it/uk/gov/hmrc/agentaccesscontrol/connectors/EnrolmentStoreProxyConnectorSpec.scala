package uk.gov.hmrc.agentaccesscontrol.connectors

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.JsResultException
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.stubs.EnrolmentStoreProxyStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testEmpRef
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testProviderId
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testSaUtr
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse

class EnrolmentStoreProxyConnectorSpec extends ComponentSpecHelper with EnrolmentStoreProxyStub with MetricTestSupport {

  val agentCode  = AgentCode("A1234567890A")
  val providerId = "12345-credId"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  "EnrolmentStoreProxy" when {

    "assignedSaAgents is called" should {
      "return agent assignments" in {
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(
          OK,
          successfulResponseDelegated(Seq("98741987654321", "98741987654322", testProviderId))
        )

        val assigned = await(connector.getIRSADelegatedUserIdsFor(testSaUtr)).toSeq

        assigned.head.value shouldBe "98741987654321"
        assigned(1).value shouldBe "98741987654322"
        assigned(2).value shouldBe providerId
      }

      "return empty list if there are no assigned credentials" in {
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq.empty))

        await(connector.getIRSADelegatedUserIdsFor(testSaUtr)) shouldBe empty
      }

      "return empty list if Enrolment Store Proxy returns 204" in {
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(NO_CONTENT, Json.obj())

        await(connector.getIRSADelegatedUserIdsFor(testSaUtr)) shouldBe empty
      }

      "throw exception for invalid JSON" in {
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, Json.arr())

        an[JsResultException] should be thrownBy await(connector.getIRSADelegatedUserIdsFor(testSaUtr))
      }

      "throw exception when HTTP error" in {
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(INTERNAL_SERVER_ERROR, Json.obj())

        an[UpstreamErrorResponse] should be thrownBy await(connector.getIRSADelegatedUserIdsFor(testSaUtr))
      }

      "record metrics for outbound call" in {
        stubQueryUsersAssignedEnrolmentsDelegatedSa(testSaUtr)(OK, successfulResponseDelegated(Seq(testProviderId)))

        await(connector.getIRSADelegatedUserIdsFor(testSaUtr))
        timerShouldExistAndHasBeenUpdated("ConsumedAPI-EnrolmentStoreProxy-ES0-GET")
      }
    }

    "assignedPayeAgents is called" should {
      "return agent assignments" in {
        stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(
          OK,
          successfulResponseDelegated(Seq("98741987654321", "98741987654322", testProviderId))
        )

        val assigned = await(connector.getIRPAYEDelegatedUserIdsFor(testEmpRef)).toSeq

        assigned.head.value shouldBe "98741987654321"
        assigned(1).value shouldBe "98741987654322"
        assigned(2).value shouldBe providerId
      }

      "return empty list if there are no assigned credentials" in {
        stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq.empty))

        await(connector.getIRPAYEDelegatedUserIdsFor(testEmpRef)) shouldBe empty
      }

      "return empty list if Enrolment Store Proxy returns 204" in {
        stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(NO_CONTENT, Json.obj())

        await(connector.getIRPAYEDelegatedUserIdsFor(testEmpRef)) shouldBe empty
      }

      "throw exception for invalid JSON" in {
        stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, Json.obj())

        an[JsResultException] should be thrownBy await(connector.getIRPAYEDelegatedUserIdsFor(testEmpRef))
      }

      "throw exception when HTTP error" in {
        stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(INTERNAL_SERVER_ERROR, Json.obj())

        an[UpstreamErrorResponse] should be thrownBy await(connector.getIRPAYEDelegatedUserIdsFor(testEmpRef))
      }

      "record metrics for outbound call" in {
        stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq(testProviderId)))

        await(connector.getIRPAYEDelegatedUserIdsFor(testEmpRef))
        timerShouldExistAndHasBeenUpdated("ConsumedAPI-EnrolmentStoreProxy-ES0-GET")
      }
    }
  }

}
