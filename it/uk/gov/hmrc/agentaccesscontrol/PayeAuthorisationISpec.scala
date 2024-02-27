package uk.gov.hmrc.agentaccesscontrol

import play.api.libs.json.Json
import play.api.test.Helpers._
import play.utils.UriEncoding.encodePathSegment
import uk.gov.hmrc.agentaccesscontrol.stubs.AuthStub
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.stubs.DesStub
import uk.gov.hmrc.agentaccesscontrol.stubs.EnrolmentStoreProxyStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testAgentCode
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testArn
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testEmpRef
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants.testProviderId

class PayeAuthorisationISpec
    extends ComponentSpecHelper
    with MetricTestSupport
    with AuthStub
    with DataStreamStub
    with DesStub
    with EnrolmentStoreProxyStub {

  val uri = s"/epaye-auth/agent/${testAgentCode.value}/client/${encodePathSegment(testEmpRef.value, "UTF-8")}"

  s"GET $uri" should {
    "return 200 when access is granted" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq(testProviderId)))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

      val result = get(uri)

      result.status shouldBe OK
    }

    "return 401 when access is not granted" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq.empty))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

      val result = get(uri)

      result.status shouldBe UNAUTHORIZED
    }

    "return 502 if a downstream service fails" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq(testProviderId)))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(BAD_GATEWAY, Json.obj())

      val result = get(uri)

      result.status shouldBe BAD_GATEWAY
    }
  }

  s"POST $uri" should {
    "return 200 when access is granted" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq(testProviderId)))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

      val result = post(uri)(Json.obj())

      result.status shouldBe OK
    }

    "return 401 when access is not granted" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq.empty))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

      val result = post(uri)(Json.obj())

      result.status shouldBe UNAUTHORIZED
    }

    "return 502 if a downstream service fails" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq(testProviderId)))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(BAD_GATEWAY, Json.obj())

      val result = post(uri)(Json.obj())

      result.status shouldBe BAD_GATEWAY
    }

    "record metrics for inbound http call" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubQueryUsersAssignedEnrolmentsDelegatedPaye(testEmpRef)(OK, successfulResponseDelegated(Seq(testProviderId)))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))
      cleanMetricRegistry()

      val result = post(uri)(Json.obj())

      result.status shouldBe OK
      timerShouldExistAndHasBeenUpdated("API-__epaye-auth__agent__:__client__:-POST")
    }
  }

}
