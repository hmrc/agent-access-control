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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import play.api.http.Status.NOT_FOUND
import play.api.libs.json._
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.models.Arn
import uk.gov.hmrc.agentaccesscontrol.models.ClientIdentifier
import uk.gov.hmrc.agentaccesscontrol.models.Service
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse

case class Relationship(arn: String, clientId: String)

object Relationship {
  implicit val jsonReads: Reads[Relationship] = Json.reads[Relationship]
}

@ImplementedBy(classOf[RelationshipsConnectorImpl])
trait RelationshipsConnector {
  def relationshipExists(arn: Arn, maybeUserId: Option[String], identifier: TaxIdentifier, service: Service)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier
  ): Future[Boolean]

}

@Singleton
class RelationshipsConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)
    extends RelationshipsConnector {

  def relationshipExists(arn: Arn, maybeUserId: Option[String], identifier: TaxIdentifier, service: Service)(
      implicit ec: ExecutionContext,
      hc: HeaderCarrier
  ): Future[Boolean] = {

    val identifierTypeId = ClientIdentifier(identifier).enrolmentId

    val urlParam = maybeUserId.fold("")(userId => s"?userId=$userId")
    val relationshipUrl =
      s"${appConfig.acrBaseUrl}/agent-client-relationships/agent/${arn.value}/service/${service.id}/client/$identifierTypeId/${identifier.value}$urlParam"

    httpClient.get(url"$relationshipUrl").execute[HttpResponse].map { response =>
      response.status match {
        case status if is2xx(status) => true
        case NOT_FOUND               => false
        case _ =>
          throw UpstreamErrorResponse(s"Error calling: $relationshipUrl", response.status)
      }
    }
  }
}
