package uk.gov.hmrc.agentaccesscontrol

import play.api.libs.json.Json
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.NO_CONTENT
import play.api.test.Helpers.OK
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentClientAuthorisationStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentClientRelationshipStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentPermissionsStub
import uk.gov.hmrc.agentaccesscontrol.stubs.AuthStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.MetricTestSupport
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._

class StandardServicesAuthorisationISpec
    extends ComponentSpecHelper
    with MetricTestSupport
    with AuthStub
    with AgentClientAuthorisationStub
    with AgentClientRelationshipStub
    with AgentPermissionsStub {

  private val NoRelationship = "NO_RELATIONSHIP"
  private val NoAssignment   = "NO_ASSIGNMENT"

  private val mtdItAuth   = "mtd-it-auth"
  private val mtdItRegime = "ITSA"
  private val mtdItIdUri  = s"/$mtdItAuth/agent/${testAgentCode.value}/client/${testMtdItId.value}"

  private val mtdVatAuth   = "mtd-vat-auth"
  private val mtdVatRegime = "VATC"
  private val mtdVatUri    = s"/$mtdVatAuth/agent/${testAgentCode.value}/client/${testVrn.value}"

  private val cgtAuth   = "cgt-auth"
  private val cgtRegime = "CGT"
  private val cgtUri    = s"/$cgtAuth/agent/${testAgentCode.value}/client/${testCgtRef.value}"

  private val pptAuth   = "ppt-auth"
  private val pptRegime = "PPT"
  private val pptUri    = s"/$pptAuth/agent/${testAgentCode.value}/client/${testPptRef.value}"

  private val trustAuth          = "trust-auth"
  private val trustRegime        = "TRS"
  private val trustUri           = s"/$trustAuth/agent/${testAgentCode.value}/client/${testUtr.value}"
  private val nonTaxableTrustUri = s"/$trustAuth/agent/${testAgentCode.value}/client/${testUrn.value}"

  s"GET $mtdItIdUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)

      val result = get(mtdItIdUri)

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)

        val result = get(mtdItIdUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
        stubMtdItAgentClientRelationship(testArn, testMtdItId)(NOT_FOUND)

        val result = get(mtdItIdUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is a relationship between agency and client but access groups are enabled and user is not assigned to client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
        stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
        stubMtdItAgentClientRelationshipToUser(testArn, testMtdItId, testProviderId)(NOT_FOUND)

        val result = get(mtdItIdUri)

        result.status shouldBe 401
        result.body should include(NoAssignment)
      }

    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      val result = get(mtdItIdUri)

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdItAuth}__agent__:__client__:-GET")
    }

    "handle suspended for regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, mtdItRegime)
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      val result = get(mtdItIdUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdItAuth}__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, "AGSV")
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      val result = get(mtdItIdUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdItAuth}__agent__:__client__:-GET")
    }
  }

  s"POST $mtdItIdUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)

      val result = post(mtdItIdUri)(Json.obj())

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)

        val result = post(mtdItIdUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
        stubMtdItAgentClientRelationship(testArn, testMtdItId)(NOT_FOUND)

        val result = post(mtdItIdUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdItRegime)
      stubMtdItAgentClientRelationship(testArn, testMtdItId)(OK)
      cleanMetricRegistry()

      val result = post(mtdItIdUri)(Json.obj())

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdItAuth}__agent__:__client__:-POST")
    }
  }

  s"GET $mtdVatUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)

      val result = get(mtdVatUri)

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)

        val result = get(mtdVatUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
        stubMtdVatAgentClientRelationship(testArn, testVrn)(NOT_FOUND)

        val result = get(mtdVatUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is a relationship between agency and client but access groups are enabled and user is not assigned to client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
        stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
        stubMtdVatAgentClientRelationshipToUser(testArn, testVrn, testProviderId)(NOT_FOUND)

        val result = get(mtdVatUri)

        result.status shouldBe 401
        result.body should include(NoAssignment)
      }

    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
      cleanMetricRegistry()

      val result = get(mtdVatUri)

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdVatAuth}__agent__:__client__:-GET")
    }

    "handle suspended for regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, mtdVatRegime)
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
      cleanMetricRegistry()

      val result = get(mtdVatUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdVatAuth}__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, "AGSV")
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
      cleanMetricRegistry()

      val result = get(mtdVatUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdVatAuth}__agent__:__client__:-GET")
    }
  }

  s"POST $mtdVatUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)

      val result = post(mtdVatUri)(Json.obj())

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)

        val result = post(mtdVatUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
        stubMtdVatAgentClientRelationship(testArn, testVrn)(NOT_FOUND)

        val result = post(mtdVatUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, mtdVatRegime)
      stubMtdVatAgentClientRelationship(testArn, testVrn)(OK)
      cleanMetricRegistry()

      val result = post(mtdVatUri)(Json.obj())

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${mtdVatAuth}__agent__:__client__:-POST")
    }
  }

  s"GET $cgtUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)

      val result = get(cgtUri)

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)

        val result = get(cgtUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(NOT_FOUND)

        val result = get(cgtUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is a relationship between agency and client but access groups are enabled and user is not assigned to client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
        stubCgtAgentClientRelationshipToUser(testArn, testCgtRef, testProviderId)(NOT_FOUND)

        val result = get(cgtUri)

        result.status shouldBe 401
        result.body should include(NoAssignment)
      }

    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
      cleanMetricRegistry()

      val result = get(cgtUri)

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${cgtAuth}__agent__:__client__:-GET")
    }

    "handle suspended for regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, cgtRegime)
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
      cleanMetricRegistry()

      val result = get(cgtUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${cgtAuth}__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, "AGSV")
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
      cleanMetricRegistry()

      val result = get(cgtUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${cgtAuth}__agent__:__client__:-GET")
    }
  }

  s"POST $cgtUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)

      val result = post(cgtUri)(Json.obj())

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)

        val result = post(cgtUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
        stubCgtAgentClientRelationship(testArn, testCgtRef)(NOT_FOUND)

        val result = post(cgtUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, cgtRegime)
      stubCgtAgentClientRelationship(testArn, testCgtRef)(OK)
      cleanMetricRegistry()

      val result = post(cgtUri)(Json.obj())

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${cgtAuth}__agent__:__client__:-POST")
    }
  }

  s"GET $pptUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)

      val result = get(pptUri)

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)

        val result = get(pptUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
        stubPptAgentClientRelationship(testArn, testPptRef)(NOT_FOUND)

        val result = get(pptUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is a relationship between agency and client but access groups are enabled and user is not assigned to client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
        stubPptAgentClientRelationship(testArn, testPptRef)(OK)
        stubPptAgentClientRelationshipToUser(testArn, testPptRef, testProviderId)(NOT_FOUND)

        val result = get(pptUri)

        result.status shouldBe 401
        result.body should include(NoAssignment)
      }

    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)
      cleanMetricRegistry()

      val result = get(pptUri)

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${pptAuth}__agent__:__client__:-GET")
    }

    "handle suspended for regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, pptRegime)
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)
      cleanMetricRegistry()

      val result = get(pptUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${pptAuth}__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, "AGSV")
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)
      cleanMetricRegistry()

      val result = get(pptUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${pptAuth}__agent__:__client__:-GET")
    }
  }

  s"POST $pptUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)

      val result = post(pptUri)(Json.obj())

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)

        val result = post(pptUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
        stubPptAgentClientRelationship(testArn, testPptRef)(NOT_FOUND)

        val result = post(pptUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, pptRegime)
      stubPptAgentClientRelationship(testArn, testPptRef)(OK)
      cleanMetricRegistry()

      val result = post(pptUri)(Json.obj())

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${pptAuth}__agent__:__client__:-POST")
    }
  }

  s"GET $trustUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersAgentClientRelationship(testArn, testUtr)(OK)

      val result = get(trustUri)

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)

        val result = get(trustUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
        stubTersAgentClientRelationship(testArn, testUtr)(NOT_FOUND)

        val result = get(trustUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is a relationship between agency and client but access groups are enabled and user is not assigned to client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
        stubTersAgentClientRelationship(testArn, testUtr)(OK)
        stubTersAgentClientRelationshipToUser(testArn, testUtr, testProviderId)(NOT_FOUND)

        val result = get(trustUri)

        result.status shouldBe 401
        result.body should include(NoAssignment)
      }

    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersAgentClientRelationship(testArn, testUtr)(OK)
      cleanMetricRegistry()

      val result = get(trustUri)

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-GET")
    }

    "handle suspended for regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, trustRegime)
      stubTersAgentClientRelationship(testArn, testUtr)(OK)
      cleanMetricRegistry()

      val result = get(trustUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, "AGSV")
      stubTersAgentClientRelationship(testArn, testUtr)(OK)
      cleanMetricRegistry()

      val result = get(trustUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-GET")
    }
  }

  s"POST $trustUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersAgentClientRelationship(testArn, testUtr)(OK)

      val result = post(trustUri)(Json.obj())

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)

        val result = post(trustUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
        stubTersAgentClientRelationship(testArn, testUtr)(NOT_FOUND)

        val result = post(trustUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersAgentClientRelationship(testArn, testUtr)(OK)
      cleanMetricRegistry()

      val result = post(trustUri)(Json.obj())

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-POST")
    }
  }

  s"GET $nonTaxableTrustUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)

      val result = get(nonTaxableTrustUri)

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)

        val result = get(nonTaxableTrustUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
        stubTersntAgentClientRelationship(testArn, testUrn)(NOT_FOUND)

        val result = get(nonTaxableTrustUri)

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is a relationship between agency and client but access groups are enabled and user is not assigned to client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
        stubAgentPermissionsOptInRecordExists(testArn)(NO_CONTENT)
        stubTersntAgentClientRelationship(testArn, testUrn)(OK)
        stubTersntAgentClientRelationshipToUser(testArn, testUrn, testProviderId)(NOT_FOUND)

        val result = get(nonTaxableTrustUri)

        result.status shouldBe 401
        result.body should include(NoAssignment)
      }

    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)
      cleanMetricRegistry()

      val result = get(nonTaxableTrustUri)

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-GET")
    }

    "handle suspended for regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, trustRegime)
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)
      cleanMetricRegistry()

      val result = get(nonTaxableTrustUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-GET")
    }

    "handle suspended for AGSV regime and return unauthorised" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(OK, isSuspended = true, "AGSV")
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)
      cleanMetricRegistry()

      val result = get(nonTaxableTrustUri)

      result.status shouldBe 401
      result.body should include(NoRelationship)

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-GET")
    }
  }

  s"POST $nonTaxableTrustUri" should {
    "grant access when the agency and client are subscribed to the appropriate services and have a relationship" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)

      val result = post(nonTaxableTrustUri)(Json.obj())

      result.status shouldBe 200
    }

    "not grant access" when {
      "the agency is not subscribed to the appropriate service" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)

        val result = post(nonTaxableTrustUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }

      "there is no relationship between the agency and client" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
        stubTersntAgentClientRelationship(testArn, testUrn)(NOT_FOUND)

        val result = post(nonTaxableTrustUri)(Json.obj())

        result.status shouldBe 401
        result.body should include(NoRelationship)
      }
    }

    "record metrics for access control request" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubAgentClientAuthorisationSuspensionStatus(testArn)(NO_CONTENT, isSuspended = false, trustRegime)
      stubTersntAgentClientRelationship(testArn, testUrn)(OK)
      cleanMetricRegistry()

      val result = post(nonTaxableTrustUri)(Json.obj())

      result.status shouldBe 200

      timerShouldExistAndHasBeenUpdated(s"API-__${trustAuth}__agent__:__client__:-POST")
    }
  }
}
