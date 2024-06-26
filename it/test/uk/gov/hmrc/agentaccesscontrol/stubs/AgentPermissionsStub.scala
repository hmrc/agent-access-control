/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.TaxGroup

trait AgentPermissionsStub extends WiremockMethods {

  def stubAgentPermissionsOptInRecordExists(arn: Arn)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-permissions/arn/${arn.value}/optin-record-exists"
    ).thenReturn(status)

  def stubGetAgentPermissionTaxGroup(arn: Arn, taxService: String)(status: Int, taxGroup: TaxGroup): StubMapping =
    when(
      method = GET,
      uri = s"/agent-permissions/arn/${arn.value}/tax-group/$taxService"
    ).thenReturn(status, taxGroup)

}
