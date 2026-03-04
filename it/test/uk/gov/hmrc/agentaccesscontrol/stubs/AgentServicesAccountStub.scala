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

package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods

trait AgentServicesAccountStub extends WiremockMethods {

  def stubAgentNotSuspended: StubMapping = stubAgentServicesAccountSuspensionStatus(NO_CONTENT)

  def stubAgentServicesAccountSuspensionStatus(responseStatus: Int): StubMapping =
    when(
      method = GET,
      uri = "/agent-services-account/agent-record-with-checks"
    ).thenReturn(
      status = responseStatus
    )

  def stubAgentIsSuspended(regime: String): StubMapping =
    when(
      method = GET,
      uri = "/agent-services-account/agent-record-with-checks"
    ).thenReturn(
      status = OK,
      body = Json.obj("suspensionDetails" -> Json.obj("suspensionStatus" -> true, "regimes" -> Json.arr(regime)))
    )
}
