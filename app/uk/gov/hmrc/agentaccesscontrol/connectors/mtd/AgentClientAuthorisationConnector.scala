/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model.{
  SuspensionDetails,
  SuspensionDetailsNotFound
}
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpResponse,
  UpstreamErrorResponse
}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AgentClientAuthorisationConnectorImpl])
trait AgentClientAuthorisationConnector {
  def getSuspensionDetails(agentId: TaxIdentifier)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[SuspensionDetails]
}

class AgentClientAuthorisationConnectorImpl @Inject()(http: HttpClient)(
    implicit val metrics: Metrics,
    appConfig: AppConfig)
    extends AgentClientAuthorisationConnector {

  def getSuspensionDetails(agentId: TaxIdentifier)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[SuspensionDetails] = {

    val url =
      s"${appConfig.acaBaseUrl}/agent-client-authorisation/client/suspension-details/${agentId.value}"

    val timer = metrics.defaultRegistry.timer(
      "Timer-ConsumerAPI-Get-AgencySuspensionDetails-GET")

    timer.time()
    http.GET[HttpResponse](url).map { response =>
      timer.time().stop()
      response.status match {
        case OK         => Json.parse(response.body).as[SuspensionDetails]
        case NO_CONTENT => SuspensionDetails(suspensionStatus = false, None)
        case NOT_FOUND =>
          throw SuspensionDetailsNotFound("No record found for this agent")
        case _ =>
          throw UpstreamErrorResponse(
            s"Error ${response.status} unable to get suspension details",
            response.status)
      }
    }
  }
}
