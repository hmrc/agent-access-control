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

package uk.gov.hmrc.agentaccesscontrol.connectors.desapi

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import play.api.http.Status._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.models._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

@ImplementedBy(classOf[DesAgentClientApiConnectorImpl])
trait DesAgentClientApiConnector {
  def getSaAgentClientRelationship(saAgentReference: SaAgentReference, saUtr: SaUtr)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[SaDesAgentClientFlagsApiResponse]

  def getPayeAgentClientRelationship(agentCode: AgentCode, empRef: EmpRef)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[PayeDesAgentClientFlagsApiResponse]
}

@Singleton
class DesAgentClientApiConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClientV2, metrics: Metrics)
    extends DesAgentClientApiConnector {

  private val desBaseUrlPaye     = appConfig.desPayeUrl
  private val desBaseUrlSa       = appConfig.desSAUrl
  private val authorizationToken = appConfig.desToken
  private val environment        = appConfig.desEnv

  private val _Environment   = "Environment"
  private val _CorrelationId = "CorrelationId"
  private val _Authorization = "Authorization"

  private def explicitDesHeaders: Seq[(String, String)] =
    Seq(
      _Environment   -> environment,
      _CorrelationId -> UUID.randomUUID().toString,
      _Authorization -> s"Bearer $authorizationToken"
    )

  private implicit val foundResponseReads: Reads[SaFoundResponse] =
    (__ \ "Auth_64-8").read[Boolean].and((__ \ "Auth_i64-8").read[Boolean])(SaFoundResponse)

  private implicit val payeFoundResponseReads: Reads[PayeFoundResponse] =
    (__ \ "Auth_64-8").read[Boolean].map(PayeFoundResponse.apply)

  def getSaAgentClientRelationship(
      saAgentReference: SaAgentReference,
      saUtr: SaUtr
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SaDesAgentClientFlagsApiResponse] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

    val url   = url"$desBaseUrlSa/sa/agents/${saAgentReference.value}/client/$saUtr"
    val timer = metrics.defaultRegistry.timer(s"Timer-ConsumedAPI-DES-GetSaAgentClientRelationship-GET")

    timer.time()
    httpClient
      .get(url)
      .setHeader(explicitDesHeaders: _*)
      .execute[HttpResponse]
      .map { response =>
        timer.time().stop()
        response.status match {
          case status if is2xx(status) =>
            foundResponseReads.reads(Json.parse(response.body)).get
          case NOT_FOUND => SaNotFoundResponse
          case _ =>
            throw UpstreamErrorResponse(
              s"Error calling in getSaAgentClientRelationship at: $url",
              response.status,
              if (response.status == INTERNAL_SERVER_ERROR) BAD_GATEWAY
              else response.status
            )
        }
      }

  }

  def getPayeAgentClientRelationship(
      agentCode: AgentCode,
      empRef: EmpRef
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PayeDesAgentClientFlagsApiResponse] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ _Environment -> environment
    )
    val url =
      url"$desBaseUrlPaye/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}"
    val timer = metrics.defaultRegistry.timer(s"Timer-ConsumedAPI-DES-GetPayeAgentClientRelationship-GET")

    timer.time()
    httpClient
      .get(url)(desHeaderCarrier)
      .execute[HttpResponse]
      .map { response =>
        timer.time().stop()
        response.status match {
          case status if is2xx(status) =>
            payeFoundResponseReads.reads(Json.parse(response.body)).get
          case NOT_FOUND => PayeNotFoundResponse
          case _ =>
            throw UpstreamErrorResponse(
              s"Error calling in getPayeAgentClientRelationship at: $url",
              response.status,
              if (response.status == INTERNAL_SERVER_ERROR) BAD_GATEWAY
              else response.status
            )
        }
      }
  }

}
