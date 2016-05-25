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

package uk.gov.hmrc.agentaccesscontrol.connectors.desapi

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.model.{DesAgentClientFlagsApiResponse, FoundResponse, NotFoundResponse}
import uk.gov.hmrc.domain.{AgentCode, SaUtr}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpReads, NotFoundException}

import scala.concurrent.Future

class DesAgentClientApiConnector(desBaseUrl: String) {

  private implicit val foundResponseReads: Reads[FoundResponse] = (
    (__ \ "Auth_64-8").read[Boolean] and
    (__ \ "Auth_i64-8").read[Boolean]
    ) (FoundResponse)

  private implicit val reader = HttpReads.readFromJson[FoundResponse]

  def getAgentClientRelationship(agentCode: AgentCode, saUtr: SaUtr)(implicit hc: HeaderCarrier):
      Future[DesAgentClientFlagsApiResponse] =
    WSHttp.GET(urlFor(agentCode, saUtr)) recover {
      case _: NotFoundException => NotFoundResponse
    }

  private def urlFor(agentCode: AgentCode, saUtr: SaUtr): String =
    s"$desBaseUrl/agents/$agentCode/sa/client/sa-utr/$saUtr"
}
