/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentaccesscontrol.connectors.desapi

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.models.PayeFoundResponse
import uk.gov.hmrc.agentaccesscontrol.models.PayeNotFoundResponse
import uk.gov.hmrc.agentaccesscontrol.models.SaFoundResponse
import uk.gov.hmrc.agentaccesscontrol.models.SaNotFoundResponse
import uk.gov.hmrc.agentaccesscontrol.stubs.AuthStub
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentaccesscontrol.stubs.DesStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentaccesscontrol.utils.TestConstants._
import uk.gov.hmrc.http.HeaderCarrier

class DesAgentClientApiConnectorISpec extends ComponentSpecHelper with AuthStub with DataStreamStub with DesStub {

  implicit val headerCarrier: HeaderCarrier       = HeaderCarrier()
  implicit val ec: ExecutionContextExecutor       = ExecutionContext.global
  val desApiConnector: DesAgentClientApiConnector = app.injector.instanceOf[DesAgentClientApiConnector]

  "getSaAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
        OK,
        successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
      )

      val response = await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr))
      response shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8 and i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
        )

        await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)) shouldBe SaFoundResponse(
          auth64_8 = true,
          authI64_8 = true
        )
      }
      "agent is authorised by only i64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = true)
        )

        await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)) shouldBe SaFoundResponse(
          auth64_8 = false,
          authI64_8 = true
        )
      }
      "agent is authorised by only 64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = false)
        )

        await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)) shouldBe SaFoundResponse(
          auth64_8 = true,
          authI64_8 = false
        )
      }
      "agent is not authorised" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
          OK,
          successfulDesSaResponse(auth_64_8 = false, auth_i64_8 = false)
        )

        await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)) shouldBe SaFoundResponse(
          auth64_8 = false,
          authI64_8 = false
        )
      }
    }

    "return NotFoundResponse in case of a 404" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(NOT_FOUND, Json.obj())

      await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)) shouldBe SaNotFoundResponse
    }

    "fail in any other cases, like internal server error" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(INTERNAL_SERVER_ERROR, Json.obj())

      an[Exception] should be thrownBy await(
        desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)
      )
    }

    "log metrics for the outbound call" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesSaAgentClientRelationship(testSaAgentReference, testSaUtr)(
        OK,
        successfulDesSaResponse(auth_64_8 = true, auth_i64_8 = true)
      )

      await(desApiConnector.getSaAgentClientRelationship(testSaAgentReference, testSaUtr)) shouldBe SaFoundResponse(
        auth64_8 = true,
        authI64_8 = true
      )
    }
  }

  "getPayeAgentClientRelationship" should {
    "request DES API with the correct auth tokens" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

      val response = await(desApiConnector.getPayeAgentClientRelationship(testAgentCode, testEmpRef))
      response shouldBe PayeFoundResponse(auth64_8 = true)
    }

    "pass along 64-8 and i64-8 information" when {
      "agent is authorised by 64-8" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

        await(desApiConnector.getPayeAgentClientRelationship(testAgentCode, testEmpRef)) shouldBe PayeFoundResponse(
          auth64_8 = true
        )
      }
      "agent is not authorised" in {
        stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
        stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = false))

        await(desApiConnector.getPayeAgentClientRelationship(testAgentCode, testEmpRef)) shouldBe PayeFoundResponse(
          auth64_8 = false
        )
      }
    }

    "return NotFoundResponse in case of a 404" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(NOT_FOUND, Json.obj())

      await(desApiConnector.getPayeAgentClientRelationship(testAgentCode, testEmpRef)) shouldBe PayeNotFoundResponse
    }

    "fail in any other cases, like internal server error" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(INTERNAL_SERVER_ERROR, Json.obj())

      an[Exception] should be thrownBy await(desApiConnector.getPayeAgentClientRelationship(testAgentCode, testEmpRef))
    }

    "log metrics for outbound call" in {
      stubAuth(OK, successfulAuthResponse(testAgentCode.value, testProviderId, Some(testArn), None))
      stubDesPayeAgentClientRelationship(testAgentCode, testEmpRef)(OK, successfulDesPayeResponse(auth_64_8 = true))

      await(desApiConnector.getPayeAgentClientRelationship(testAgentCode, testEmpRef)) shouldBe PayeFoundResponse(
        auth64_8 = true
      )
    }
  }
}
