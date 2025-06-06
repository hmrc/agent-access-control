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

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.http.Status.NOT_FOUND
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetailsNotFound
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class AgentAssuranceConnector @Inject() (http: HttpClientV2)(
    implicit val metrics: Metrics,
    appConfig: AppConfig
) {

  def getSuspensionDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionDetails] = {

    val url   = url"${appConfig.agentAssuranceBaseUrl}/agent-assurance/agent/verify-entity"
    val timer = metrics.defaultRegistry.timer("Timer-ConsumerAPI-AA-AgencySuspensionDetails-POST")

    timer.time()
    http.post(url).execute[HttpResponse].map { response =>
      timer.time().stop()
      response.status match {
        case OK         => Json.parse(response.body).as[SuspensionDetails]
        case NO_CONTENT => SuspensionDetails(suspensionStatus = false, None)
        case NOT_FOUND =>
          throw SuspensionDetailsNotFound("No record found for this agent")
        case _ =>
          throw UpstreamErrorResponse(s"Error ${response.status} unable to get suspension details", response.status)
      }
    }
  }
}
