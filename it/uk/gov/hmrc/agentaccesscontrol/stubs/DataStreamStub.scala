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

package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockHelper.stubPost
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockHelper.verifyPost

trait DataStreamStub extends Eventually {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private def auditUrl = "/write/audit"

  def givenAuditConnector(): Seq[StubMapping] =
    List(
      stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(204))),
      stubFor(post(urlPathEqualTo(auditUrl + "/merged")).willReturn(aResponse().withStatus(204)))
    )
  def stubAudit(): StubMapping = {
    stubPost("/write/audit", 204, "{}")
    stubPost("/write/audit/merged", 204, "{}")
  }

  def verifyAudit(): Unit = {
    verifyPost("/write/audit")
    verifyPost("/write/audit/merged")
  }

}
