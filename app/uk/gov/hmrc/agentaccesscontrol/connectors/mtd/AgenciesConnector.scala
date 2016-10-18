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

package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import java.net.URL

import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.model.Arn
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

case class AgencyRecord(arn: Arn)
object AgencyRecord {
  implicit val reads = Json.reads[AgencyRecord]
}

class AgenciesConnector(baseUrl: URL, httpGet: HttpGet) {
  def fetchAgencyRecord(agentCode: AgentCode)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[AgencyRecord]] = {
    httpGet.GET[Option[AgencyRecord]](agencyUrl(agentCode).toString)
  }

  private def agencyUrl(agentCode: AgentCode) =
    new URL(baseUrl, s"/agencies-fake/agencies/agentcode/$agentCode")
}
