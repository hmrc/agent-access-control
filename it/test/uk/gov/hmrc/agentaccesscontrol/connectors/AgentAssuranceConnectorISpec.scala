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

import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentaccesscontrol.stubs.AgentAssuranceStub
import uk.gov.hmrc.agentaccesscontrol.utils.ComponentSpecHelper
import uk.gov.hmrc.agentmtdidentifiers.model.{SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class AgentAssuranceConnectorISpec extends ComponentSpecHelper with AgentAssuranceStub {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val connector: AgentAssuranceConnector = app.injector.instanceOf[AgentAssuranceConnector]

  "getSuspensionDetails" should {

    "return a SuspensionDetails model containing details from the response when the status is 200" in {
      stubAgentIsSuspended("HMRC-MTD-VAT")
      val result = await(connector.getSuspensionDetails)

      result shouldBe SuspensionDetails(suspensionStatus = true, Some(Set("HMRC-MTD-VAT")))
    }

    "return a SuspensionDetails model with default details when the status is 204 (not suspended)" in {
      stubAgentNotSuspended
      val result = await(connector.getSuspensionDetails)

      result shouldBe SuspensionDetails(suspensionStatus = false, None)
    }

    "throw a SuspensionDetailsNotFound exception when the status is 404" in {
      stubAgentAssuranceSuspensionStatus(404)

      intercept[SuspensionDetailsNotFound](await(connector.getSuspensionDetails))
    }

    "throw a UpstreamErrorResponse exception when the status is unexpected" in {
      stubAgentAssuranceSuspensionStatus(500)

      intercept[UpstreamErrorResponse](await(connector.getSuspensionDetails))
    }
  }
}
