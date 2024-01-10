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

package uk.gov.hmrc.agentaccesscontrol.connectors

import com.kenshoo.play.metrics.Metrics
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpResponse,
  UpstreamErrorResponse
}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AfiRelationshipConnector @Inject()(appConfig: AppConfig,
                                         httpClient: HttpClient,
                                         metrics: Metrics) {

  def hasRelationship(arn: String, clientId: String)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Boolean] = {

    val timer =
      metrics.defaultRegistry.timer(
        "Timer-ConsumedAPI-AgentFiRelationship-Check-GET")

    val afiRelationshipUrl =
      new URL(
        s"${appConfig.afiBaseUrl}/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/$arn/client/$clientId").toString

    timer.time()
    httpClient.GET[HttpResponse](afiRelationshipUrl).map { response =>
      timer.time().stop()
      response.status match {
        case status if is2xx(status) => true
        case NOT_FOUND               => false
        case _ =>
          throw UpstreamErrorResponse(s"Error calling: $afiRelationshipUrl",
                                      response.status)
      }
    }
  }

}
