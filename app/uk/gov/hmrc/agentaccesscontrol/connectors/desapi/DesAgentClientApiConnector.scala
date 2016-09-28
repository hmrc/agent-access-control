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
import uk.gov.hmrc.agentaccesscontrol.model.{DesAgentClientFlagsApiResponse, FoundResponse, NotFoundResponse}
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, SaUtr}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization

import scala.concurrent.Future

class DesAgentClientApiConnector(desBaseUrl: String, authorizationToken: String, environment: String, httpGet: HttpGet) {

  private implicit val foundResponseReads: Reads[FoundResponse] = (
    (__ \ "Auth_64-8").read[Boolean] and
    (__ \ "Auth_i64-8").read[Boolean]
    ) (FoundResponse)

  def getAgentClientRelationship(saAgentReference: SaAgentReference, agentCode: AgentCode, saUtr: SaUtr)(implicit hc: HeaderCarrier):
        Future[DesAgentClientFlagsApiResponse] = {
    val url: String = urlFor(saAgentReference, saUtr)
    getWithDesHeaders(url) map { r =>
      foundResponseReads.reads(Json.parse(r.body)).get
    } recover {
      case _: NotFoundException => NotFoundResponse
    }
  }

  private def urlFor(saAgentReference: SaAgentReference, saUtr: SaUtr): String =
    s"$desBaseUrl/sa/agents/${saAgentReference.value}/client/$saUtr"

  private def getWithDesHeaders(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ "env" -> environment)
    httpGet.GET[HttpResponse](url)(implicitly[HttpReads[HttpResponse]], desHeaderCarrier)
  }
}
