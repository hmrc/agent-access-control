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

import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.{BeforeAndAfterEach, Suite, BeforeAndAfter}
import uk.gov.hmrc.agentaccesscontrol.StartAndStopWireMock
import uk.gov.hmrc.domain.{SaUtr, AgentCode}

trait TestObjects {
  val theAgent = AgentCode("Agent1")
  val unknownAgent = AgentCode("UnknownAgent")
  val agent500 = AgentCode("Agent500")
  val anUnauthorisedClient = SaUtr("ClientOfAgent0")
  val aClientAuthorisedBy648 = SaUtr("ClientOfAgent1")
  val aClientAuthorisedByI648 = SaUtr("ClientOfAgent2")
  val aFullyAuthorisedClient = SaUtr("ClientOfAgent3")
  val anUnlinkedClient = SaUtr("UnknownClient")
}

trait DesStubHelper {

  def desAgentClientFlagsRequest =
    new DesAgentClientFlagsStubBuilder(get(urlMatching(s"/agents/[^/]+/sa/client/sa-utr/[^/]+")))

  def desAgentClientFlagsRequest(agentCode: AgentCode) =
    new DesAgentClientFlagsStubBuilder(get(urlMatching(s"/agents/${agentCode.value}/sa/client/sa-utr/[^/]+")))

  def desAgentClientFlagsRequest(agentCode: AgentCode, clientSaUtr: SaUtr) =
    new DesAgentClientFlagsStubBuilder(get(urlPathEqualTo(s"/agents/${agentCode.value}/sa/client/sa-utr/${clientSaUtr.value}")))

  case class DesAgentClientFlagsStubBuilder(request: MappingBuilder) {

    def willReturn(rdb: ResponseDefinitionBuilder) = stubFor(request.willReturn(rdb))

    def willReturnStatus(st: Int) = willReturn(aResponse().withStatus(st))

    def willReturnFlags(auth_64_8: Boolean, auth_i64_8: Boolean) =
      willReturn(aResponse().withStatus(200).withBody(
        s"""
          |{
          |    "Auth_64-8": $auth_64_8,
          |    "Auth_i64-8": $auth_i64_8
          |}
        """.stripMargin))
  }

}

trait DesStub extends DesStubHelper with BeforeAndAfterEach with TestObjects {
  me: StartAndStopWireMock with Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    desAgentClientFlagsRequest(theAgent, anUnauthorisedClient).willReturnFlags(false, false)
    desAgentClientFlagsRequest(theAgent, aClientAuthorisedBy648).willReturnFlags(true, false)
    desAgentClientFlagsRequest(theAgent, aClientAuthorisedByI648).willReturnFlags(false, true)
    desAgentClientFlagsRequest(theAgent, aFullyAuthorisedClient).willReturnFlags(true, true)
    desAgentClientFlagsRequest(unknownAgent).willReturnStatus(404)
    desAgentClientFlagsRequest(agent500).willReturnStatus(500)
  }
}
