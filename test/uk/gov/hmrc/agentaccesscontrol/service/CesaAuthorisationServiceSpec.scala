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

package uk.gov.hmrc.agentaccesscontrol.service

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.model.{FoundResponse, NotFoundResponse}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class CesaAuthorisationServiceSpec extends UnitSpec with MockitoSugar {
  val agentCode = AgentCode("ABCDEF123456")
  val saAgentRef = SaAgentReference("ABC456")
  val clientSaUtr = SaUtr("CLIENTSAUTR456")


  implicit val headerCarrier = HeaderCarrier()

  "isAuthorisedInCesa" should {
    "return false if the Agent or the relationship between the Agent and Client was not found in DES" in new Context {
      when(mockDesAgentClientApiConnector.getAgentClientRelationship(saAgentRef, agentCode, clientSaUtr)).
        thenReturn(NotFoundResponse)

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "return true if the DES API returns 64-8=true and i64-8=true" in new Context {
      when(mockDesAgentClientApiConnector.getAgentClientRelationship(saAgentRef, agentCode, clientSaUtr)).
        thenReturn(FoundResponse(auth64_8 = true, authI64_8 = true))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe true
    }

    "return false if the DES API returns 64-8=true and i64-8=false" in new Context {
      when(mockDesAgentClientApiConnector.getAgentClientRelationship(saAgentRef, agentCode, clientSaUtr)).
        thenReturn(FoundResponse(auth64_8 = true, authI64_8 = false))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "return false if the DES API returns 64-8=false and i64-8=true" in new Context {
      when(mockDesAgentClientApiConnector.getAgentClientRelationship(saAgentRef, agentCode, clientSaUtr)).
        thenReturn(FoundResponse(auth64_8 = false, authI64_8 = true))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "return false if the DES API returns 64-8=false and i64-8=false" in new Context {
      when(mockDesAgentClientApiConnector.getAgentClientRelationship(saAgentRef, agentCode, clientSaUtr)).
        thenReturn(FoundResponse(auth64_8 = false, authI64_8 = false))

      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) shouldBe false
    }

    "propagate any errors that happened" in new Context {
      when(mockDesAgentClientApiConnector.getAgentClientRelationship(saAgentRef, agentCode, clientSaUtr)).
        thenReturn(Future failed new BadRequestException("bad request"))

      intercept[BadRequestException] {
        await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
      }
    }
  }

  private abstract class Context {
    val mockDesAgentClientApiConnector = mock[DesAgentClientApiConnector]
    val service = new CesaAuthorisationService(mockDesAgentClientApiConnector)
  }
}
