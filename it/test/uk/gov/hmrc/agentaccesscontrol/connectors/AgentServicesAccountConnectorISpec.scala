/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.connectors

import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import uk.gov.hmrc.agentaccesscontrol.models.SuspensionDetails
import uk.gov.hmrc.agentaccesscontrol.models.SuspensionDetailsNotFound
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentServicesAccountStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse

class AgentServicesAccountConnectorISpec extends ComponentSpecHelper with AgentServicesAccountStub {

  given hc: HeaderCarrier                      = HeaderCarrier()
  val connector: AgentServicesAccountConnector = app.injector.instanceOf[AgentServicesAccountConnector]

  "getSuspensionDetails" should {

    "return a SuspensionDetails model containing details from the response when the status is 200" in {
      stubAgentIsSuspended("HMRC-MTD-VAT")
      val result = await(connector.getSuspensionDetails)

      result shouldBe SuspensionDetails(suspensionStatus = true, Some(Set("HMRC-MTD-VAT")))
    }

    "return a SuspensionDetails model with default details when the response is 200 without suspension details" in {
      stubAgentNotSuspended
      val result = await(connector.getSuspensionDetails)

      result shouldBe SuspensionDetails(suspensionStatus = false, None)
    }

    "throw a SuspensionDetailsNotFound exception when the status is 404" in {
      stubAgentServicesAccountSuspensionStatus(404)
      val exception = the[SuspensionDetailsNotFound] thrownBy await(connector.getSuspensionDetails)

      exception shouldBe SuspensionDetailsNotFound("No record found for this agent")
    }

    "throw a UpstreamErrorResponse exception when the status is unexpected" in {
      stubAgentServicesAccountSuspensionStatus(500)

      val exception = the[UpstreamErrorResponse] thrownBy await(connector.getSuspensionDetails)

      exception shouldBe UpstreamErrorResponse(
        "Error 500 unable to get suspension details",
        500,
        500,
        Map()
      )
    }
  }
}
