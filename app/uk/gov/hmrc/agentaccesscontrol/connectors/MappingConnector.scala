/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Named, Singleton}
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.Logger
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.model.AgentReferenceMappings
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MappingConnector @Inject()(@Named("agent-mapping-baseUrl") baseUrl: URL,
                                 httpGet: HttpGet,
                                 metrics: Metrics)
    extends HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getAgentMappings(key: String, arn: Arn)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[AgentReferenceMappings] =
    monitor(s"ConsumedAPI-AgentMapping-Check-$key-GET") {
      httpGet.GET[AgentReferenceMappings](genMappingUrl(key, arn).toString)
    }.recover {
      case NonFatal(_) =>
        Logger.warn("Something went wrong")
        AgentReferenceMappings.apply(List.empty)
    }

  def genMappingUrl(key: String, arn: Arn): URL =
    new URL(baseUrl, s"/agent-mapping/mappings/key/$key/arn/${arn.value}")
}
