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
import org.skyscreamer.jsonassert.JSONCompareMode
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlEvent

object DataStreamStub {

  def verifyAuditRequestSent(event: AgentAccessControlEvent, tags: Map[String, String] = Map.empty, detail: Map[String, String] = Map.empty) = {
    verify(1, postRequestedFor(urlPathEqualTo(auditUrl))
      .withRequestBody(similarToJson(
      s"""{
         |  "auditSource": "agent-access-control",
         |  "auditType": "$event",
         |  "tags": ${Json.toJson(tags)},
         |  "detail": ${Json.toJson(detail)}
         |}"""
      ))
    )
  }

  private def auditUrl = "/write/audit"

  private def similarToJson(value: String) = equalToJson(value.stripMargin, JSONCompareMode.LENIENT)

}
