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

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import play.api.Logging
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.models.AgentReferenceMappings
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

@Singleton
class MappingConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient, metrics: Metrics) extends Logging {

  def getAgentMappings(
      key: String,
      arn: Arn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AgentReferenceMappings] = {

    val timer =
      metrics.defaultRegistry.timer(s"Timer-ConsumedAPI-AgentMapping-Check-$key-GET")

    timer.time()
    httpClient
      .GET[AgentReferenceMappings](genMappingUrl(key, arn).toString)
      .map { response =>
        timer.time().stop()
        response
      }
      .recover {
        case NonFatal(_) =>
          timer.time().stop()
          logger.warn("Something went wrong")
          AgentReferenceMappings.apply(List.empty)
      }
  }

  def genMappingUrl(key: String, arn: Arn): URL =
    new URL(s"${appConfig.agentMappingBaseUrl}/agent-mapping/mappings/key/$key/arn/${arn.value}")
}
