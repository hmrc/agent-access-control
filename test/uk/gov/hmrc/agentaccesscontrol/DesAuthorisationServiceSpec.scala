/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.service

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

class DesAuthorisationServiceSpec extends UnitSpec with MockitoSugar {
  val agentCode = AgentCode("ABCDEF123456")
  val saAgentRef = SaAgentReference("ABC456")
  val clientSaUtr = SaUtr("CLIENTSAUTR456")
  val empRef = EmpRef("123", "45676890")

  implicit val headerCarrier = HeaderCarrier()

  "isAuthorisedInCesa" should {
    "return false if the Agent or the relationship between the Agent and Client was not found in DES" in new Context {
      whenDesSaEndpointIsCalled thenReturn (SaNotFoundResponse)

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "return true if the DES API returns 64-8=true and i64-8=true" in new Context {
      whenDesSaEndpointIsCalled thenReturn (SaFoundResponse(auth64_8 = true,
                                                            authI64_8 = true))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe true
    }

    "return false if the DES API returns 64-8=true and i64-8=false" in new Context {
      whenDesSaEndpointIsCalled thenReturn (SaFoundResponse(auth64_8 = true,
                                                            authI64_8 = false))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "return false if the DES API returns 64-8=false and i64-8=true" in new Context {
      whenDesSaEndpointIsCalled thenReturn (SaFoundResponse(auth64_8 = false,
                                                            authI64_8 = true))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "return false if the DES API returns 64-8=false and i64-8=false" in new Context {
      whenDesSaEndpointIsCalled thenReturn (SaFoundResponse(auth64_8 = false,
                                                            authI64_8 = false))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "propagate any errors that happened" in new Context {
      whenDesSaEndpointIsCalled thenReturn (Future failed new BadRequestException(
        "bad request"))

      intercept[BadRequestException] {
        await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
      }
    }
  }

  "isAuthorisedInEBS" should {
    "return false if the relationship is not found in DES" in new Context {
      whenDesPayeEndpointIsCalled thenReturn (Future successful PayeNotFoundResponse)

      await(service.isAuthorisedInEbs(agentCode, empRef)) shouldBe false
    }

    "return true if the 64-8=true" in new Context {
      whenDesPayeEndpointIsCalled thenReturn (Future successful PayeFoundResponse(
        auth64_8 = true))

      await(service.isAuthorisedInEbs(agentCode, empRef)) shouldBe true
    }

    "return false if the 64-8=false" in new Context {
      whenDesPayeEndpointIsCalled thenReturn (Future successful PayeFoundResponse(
        auth64_8 = false))

      await(service.isAuthorisedInEbs(agentCode, empRef)) shouldBe false
    }

    "propagate any errors that happened" in new Context {
      whenDesPayeEndpointIsCalled thenReturn (Future failed new BadRequestException(
        "bad request"))

      intercept[BadRequestException] {
        await(service.isAuthorisedInEbs(agentCode, empRef))
      }
    }

  }

  private abstract class Context {
    val mockDesAgentClientApiConnector = mock[DesAgentClientApiConnector]
    val service = new DesAuthorisationService(mockDesAgentClientApiConnector)

    protected def whenDesSaEndpointIsCalled =
      when(
        mockDesAgentClientApiConnector
          .getSaAgentClientRelationship(saAgentRef, clientSaUtr))

    protected def whenDesPayeEndpointIsCalled =
      when(
        mockDesAgentClientApiConnector.getPayeAgentClientRelationship(agentCode,
                                                                      empRef))
  }
}
