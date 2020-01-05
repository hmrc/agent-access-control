/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Named}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpGet,
  HttpResponse,
  NotFoundException
}

import scala.concurrent.{ExecutionContext, Future}

class AfiRelationshipConnector @Inject()(
    @Named("agent-fi-relationship-baseUrl") url: URL,
    httpGet: HttpGet,
    metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def hasRelationship(arn: String, clientId: String)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Boolean] = {

    val afiRelationshipUrl =
      new URL(
        url,
        s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/$arn/client/$clientId").toString

    monitor("ConsumedAPI-AgentFiRelationship-Check-GET") {
      httpGet.GET[HttpResponse](afiRelationshipUrl)
    } map { response =>
      true
    } recover {
      case _: NotFoundException => false
    }
  }
}
