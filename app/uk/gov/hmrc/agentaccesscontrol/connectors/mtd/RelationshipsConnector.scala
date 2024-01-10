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
import com.kenshoo.play.metrics.Metrics
import play.api.http.Status.NOT_FOUND
import play.api.libs.json._
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

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class Relationship(arn: String, clientId: String)

object Relationship {
  implicit val jsonReads = Json.reads[Relationship]
}

@ImplementedBy(classOf[RelationshipsConnectorImpl])
trait RelationshipsConnector {
  def relationshipExists(arn: Arn,
                         maybeUserId: Option[String],
                         identifier: TaxIdentifier)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[Boolean]

}

@Singleton
class RelationshipsConnectorImpl @Inject()(appConfig: AppConfig,
                                           httpClient: HttpClient,
                                           metrics: Metrics)
    extends RelationshipsConnector {

  //noinspection ScalaStyle
  def relationshipExists(arn: Arn,
                         maybeUserId: Option[String],
                         identifier: TaxIdentifier)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier): Future[Boolean] = {
    /* TODO - Trying to deduce the service from the identifier type alone is discouraged - it is done here
       for legacy reasons but should be avoided. Consider changing the API to explicitly pass the intended service */
    val identifierTypeId = ClientIdentifier(identifier).enrolmentId
    val service = Service.supportedServices
      .find(_.supportedClientIdType.enrolmentId == identifierTypeId)
      .map {
        case Service.CbcNonUk =>
          Service.Cbc // treat both CBC types the same, leave it for ACR to determine which
        case s => s
      }
      .getOrElse(throw new IllegalArgumentException(
        s"Tax identifier not supported by any service: $identifier"))

    val urlParam = maybeUserId.fold("")(userId => s"?userId=$userId")
    val relationshipUrl =
      new URL(
        s"${appConfig.acrBaseUrl}/agent-client-relationships/agent/${arn.value}/service/${service.id}/client/$identifierTypeId/${identifier.value}" + urlParam).toString

    val timer = metrics.defaultRegistry.timer(
      s"Timer-ConsumedAPI-AgentClientRelationships-Check${identifier.getClass.getSimpleName}-GET")

    timer.time()
    httpClient.GET[HttpResponse](relationshipUrl).map { response =>
      timer.time().stop()
      response.status match {
        case status if is2xx(status) => true
        case NOT_FOUND               => false
        case _ =>
          throw UpstreamErrorResponse(s"Error calling: $relationshipUrl",
                                      response.status)
      }
    }
  }
}
