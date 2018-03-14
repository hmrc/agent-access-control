/*
 * Copyright 2018 HM Revenue & Customs
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
import javax.inject.{ Inject, Named, Singleton }

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.libs.json._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{ Arn, MtdItId, Vrn }
import uk.gov.hmrc.domain.TaxIdentifier

import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpResponse, NotFoundException }

case class Relationship(arn: String, clientId: String)
object Relationship {
  implicit val jsonReads = Json.reads[Relationship]
}

@Singleton
class RelationshipsConnector @Inject() (
  @Named("agent-client-relationships-baseUrl") baseUrl: URL,
  httpGet: HttpGet,
  metrics: Metrics)
  extends HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def relationshipExists(arn: Arn, identifier: TaxIdentifier)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    val (serviceName, clientType, clientId) = identifier match {
      case _@ MtdItId(mtdItId) => ("HMRC-MTD-IT", "MTDITID", mtdItId)
      case _@ Vrn(vrn) => ("HMRC-MTD-VAT", "VRN", vrn)
    }

    val relationshipUrl =
      new URL(
        baseUrl,
        s"/agent-client-relationships/agent/${arn.value}/service/$serviceName/client/$clientType/$clientId").toString

    monitor(s"ConsumedAPI-AgentClientRelationships-Check${identifier.getClass.getSimpleName}-GET") {
      httpGet.GET[HttpResponse](relationshipUrl)
    } map { response =>
      true
    } recover {
      case _: NotFoundException => false
    }
  }
}
