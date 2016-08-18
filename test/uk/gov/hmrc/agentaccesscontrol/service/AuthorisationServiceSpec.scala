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
import uk.gov.hmrc.agentaccesscontrol.connectors.AuthConnector
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class AuthorisationServiceSpec extends UnitSpec with MockitoSugar {
  val agentCode = AgentCode("ABCDEF123456")
  val saAgentRef = SaAgentReference("ABC456")
  val clientSaUtr = SaUtr("CLIENTSAUTR456")


  implicit val headerCarrier = HeaderCarrier()


  "isAuthorised" should {
    "return false if SA agent reference cannot be found" in new Context {
      when(mockAuthConnector.currentSaAgentReference()).thenReturn(None)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr)) shouldBe false
    }

    "return false if SA agent reference is found and CesaAuthorisationService returns false" in new Context {
      when(mockAuthConnector.currentSaAgentReference()).thenReturn(Some(saAgentRef))
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(false)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr)) shouldBe false
    }

    "return true if SA agent reference is found and CesaAuthorisationService returns true" in new Context {
      when(mockAuthConnector.currentSaAgentReference()).thenReturn(Some(saAgentRef))
      when(mockCesaAuthorisationService.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
        .thenReturn(true)

      await(authorisationService.isAuthorised(agentCode, clientSaUtr)) shouldBe true
    }

    "propagate any errors that happened" in new Context {
      when(mockAuthConnector.currentSaAgentReference()).thenReturn(Future failed new BadRequestException("bad request"))

      intercept[BadRequestException] {
        await(authorisationService.isAuthorised(agentCode, clientSaUtr))
      }
    }
  }

  private abstract class Context {
    val mockAuthConnector = mock[AuthConnector]
    val mockCesaAuthorisationService = mock[CesaAuthorisationService]
    val authorisationService = new AuthorisationService(
      mockCesaAuthorisationService,
      mockAuthConnector)
  }
}
