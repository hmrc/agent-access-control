/*
 * Copyright 2021 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.http.Status.NOT_FOUND
import play.api.libs.json._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpResponse,
  UpstreamErrorResponse
}

import scala.concurrent.{ExecutionContext, Future}

case class Relationship(arn: String, clientId: String)

object Relationship {
  implicit val jsonReads = Json.reads[Relationship]
}

@ImplementedBy(classOf[RelationshipsConnectorImpl])
trait RelationshipsConnector {
  def relationshipExists(arn: Arn, identifier: TaxIdentifier)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[Boolean]
}

@Singleton
class RelationshipsConnectorImpl @Inject()(appConfig: AppConfig,
                                           httpClient: HttpClient,
                                           metrics: Metrics)
    extends RelationshipsConnector
    with HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def relationshipExists(arn: Arn, identifier: TaxIdentifier)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[Boolean] = {
    val (serviceName, clientType, clientId) = identifier match {
      case _ @MtdItId(mtdItId) => ("HMRC-MTD-IT", "MTDITID", mtdItId)
      case _ @Vrn(vrn)         => ("HMRC-MTD-VAT", "VRN", vrn)
      case _ @Utr(utr)         => ("HMRC-TERS-ORG", "SAUTR", utr)
      case _ @CgtRef(cgtRef)   => ("HMRC-CGT-PD", "CGTPDRef", cgtRef)
      case _ @Urn(urn)         => ("HMRC-TERSNT-ORG", "URN", urn)
    }

    val relationshipUrl =
      new URL(
        s"${appConfig.acrBaseUrl}/agent-client-relationships/agent/${arn.value}/service/$serviceName/client/$clientType/$clientId").toString

    monitor(
      s"ConsumedAPI-AgentClientRelationships-Check${identifier.getClass.getSimpleName}-GET") {
      httpClient.GET[HttpResponse](relationshipUrl)
    } map (_.status match {
      case o if is2xx(o) => true
      case NOT_FOUND     => false
      case s =>
        throw UpstreamErrorResponse(s"Error calling: $relationshipUrl", s)
    })
  }
}
