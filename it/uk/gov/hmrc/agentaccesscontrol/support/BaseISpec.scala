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

package uk.gov.hmrc.agentaccesscontrol.support

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.agentaccesscontrol.StartAndStopWireMock
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

abstract class BaseISpec extends UnitSpec
    with StartAndStopWireMock
    with StubUtils
    with OneServerPerSuite {
  implicit val hc = HeaderCarrier()
}

trait StubUtils {
  me: StartAndStopWireMock =>

  class PreconditionBuilder {
    def agentAdmin(agentCode: String, oid: String = "556737e15500005500eaf68e") = {
      new AgentAdmin(agentCode, oid)
    }
  }

  def given() = {
    new PreconditionBuilder()
  }

  class AgentAdmin(agentCode: String, oid: String) {
    def andGettingEnrolmentsFailsWith500(): AgentAdmin = {
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(500)))
      this
    }

    def andHasNoSaAgentReference(): AgentAdmin = {
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"},
           | {"key":"HMRC-AGENT-AGENT","identifiers":[{"key":"AgentRefNumber","value":"JARN1234567"}],"state":"Activated"}]
         """.stripMargin
      )))
      this
    }

    def andHasSaAgentReference(saAgentReference: String): AgentAdmin = {
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"},
           | {"key":"HMRC-AGENT-AGENT","identifiers":[{"key":"AgentRefNumber","value":"JARN1234567"}],"state":"Activated"},
           | {"key":"IR-SA-AGENT","identifiers":[{"key":"AnotherIdentifier", "value": "not the IR Agent Reference"}, {"key":"IRAgentReference","value":"$saAgentReference"}],"state":"Activated"}]
         """.stripMargin
      )))
      this
    }

    def isLoggedIn(): AgentAdmin = {
//      stubFor(get(urlPathEqualTo(s"/authorise/read/agent/$agentCode")).willReturn(aResponse().withStatus(200)))
//      stubFor(get(urlPathEqualTo(s"/authorise/write/agent/$agentCode")).willReturn(aResponse().withStatus(200)))
      stubFor(get(urlPathEqualTo(s"/auth/authority")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |{
           |  "new-session":"/auth/oid/$oid/session",
           |  "enrolments":"/auth/oid/$oid/enrolments",
           |  "uri":"/auth/oid/$oid",
           |  "loggedInAt":"2016-06-20T10:44:29.634Z",
           |  "credentials":{
           |    "gatewayId":"0000001592621267"
           |  },
           |  "accounts":{
           |    "agent":{
           |      "link":"/agent/$agentCode",
           |      "agentCode":"$agentCode",
           |      "agentUserId":"ZMOQ1hrrP-9ZmnFw0kIA5vlc-mo",
           |      "agentUserRole":"admin",
           |      "payeReference":"HZ1234",
           |      "agentBusinessUtr":"JARN1234567"
           |    },
           |    "taxsAgent":{
           |      "link":"/taxsagent/V3264H",
           |      "uar":"V3264H"
           |    }
           |  },
           |  "lastUpdated":"2016-06-20T10:44:29.634Z",
           |  "credentialStrength":"strong",
           |  "confidenceLevel":50,
           |  "userDetailsLink":"$wiremockBaseUrl/user-details/id/$oid",
           |  "levelOfAssurance":"1",
           |  "previouslyLoggedInAt":"2016-06-20T09:48:37.112Z"
           |}
       """.stripMargin
      )))
      this
    }
  }


}
