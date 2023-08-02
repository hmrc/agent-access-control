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

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import play.api.http.Status._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HttpErrorFunctions._
import uk.gov.hmrc.http._

import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DesAgentClientApiConnectorImpl])
trait DesAgentClientApiConnector {
  def getSaAgentClientRelationship(saAgentReference: SaAgentReference,
                                   saUtr: SaUtr)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[SaDesAgentClientFlagsApiResponse]

  def getPayeAgentClientRelationship(agentCode: AgentCode, empRef: EmpRef)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[PayeDesAgentClientFlagsApiResponse]
}

@Singleton
class DesAgentClientApiConnectorImpl @Inject()(appConfig: AppConfig,
                                               httpClient: HttpClient,
                                               metrics: Metrics)
    extends DesAgentClientApiConnector
    with HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val desBaseUrl = appConfig.desUrl
  val desBaseUrlPaye = appConfig.desPayeUrl
  val desBaseUrlSa = appConfig.desSAUrl
  val authorizationToken = appConfig.desToken
  val environment = appConfig.desEnv

  private val _Environment = "Environment"
  private val _CorrelationId = "CorrelationId"
  private val _Authorization = "Authorization"

  private def explicitDesHeaders =
    Seq(_Environment -> environment,
        _CorrelationId -> UUID.randomUUID().toString,
        _Authorization -> s"Bearer $authorizationToken")

  private implicit val foundResponseReads: Reads[SaFoundResponse] =
    ((__ \ "Auth_64-8").read[Boolean] and
      (__ \ "Auth_i64-8").read[Boolean])(SaFoundResponse)

  private implicit val payeFoundResponseReads: Reads[PayeFoundResponse] =
    (__ \ "Auth_64-8").read[Boolean].map(PayeFoundResponse.apply)

  def getSaAgentClientRelationship(saAgentReference: SaAgentReference,
                                   saUtr: SaUtr)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[SaDesAgentClientFlagsApiResponse] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
    val url = saUrlFor(saAgentReference, saUtr)
    getWithDesHeaders("GetSaAgentClientRelationship", url) map { r =>
      r.status match {
        case o if is2xx(o) => foundResponseReads.reads(Json.parse(r.body)).get
        case NOT_FOUND     => SaNotFoundResponse
        case s =>
          throw UpstreamErrorResponse(
            s"Error calling in getSaAgentClientRelationship at: $url",
            s,
            if (s == INTERNAL_SERVER_ERROR) BAD_GATEWAY else s)
      }
    }

  }

  def getPayeAgentClientRelationship(agentCode: AgentCode, empRef: EmpRef)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[PayeDesAgentClientFlagsApiResponse] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
    val url = payeUrlFor(agentCode, empRef)
    getWithDesPayeHeaders("GetPayeAgentClientRelationship", url) map (r =>
      r.status match {
        case o if is2xx(o) =>
          payeFoundResponseReads.reads(Json.parse(r.body)).get
        case NOT_FOUND => PayeNotFoundResponse
        case s =>
          throw UpstreamErrorResponse(
            s"Error calling in getPayeAgentClientRelationship at: $url",
            s,
            if (s == INTERNAL_SERVER_ERROR) BAD_GATEWAY else s)
      })
  }

  private def saUrlFor(saAgentReference: SaAgentReference, saUtr: SaUtr): URL =
    new URL(s"$desBaseUrlSa/sa/agents/${saAgentReference.value}/client/$saUtr")

  private def payeUrlFor(agentCode: AgentCode, empRef: EmpRef): URL =
    new URL(
      s"$desBaseUrlPaye/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}")

  private def getWithDesHeaders[A: HttpReads](apiName: String, url: URL)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[A] = {
    monitor(s"ConsumedAPI-DES-$apiName-GET") {
      httpClient
        .GET[A](url.toString, headers = explicitDesHeaders)(implicitly, hc, ec)
    }
  }

  private def getWithDesPayeHeaders[A: HttpReads](apiName: String, url: URL)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[A] = {
    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ "Environment" -> environment)
    monitor(s"ConsumedAPI-DES-$apiName-GET") {
      httpClient.GET[A](url.toString)(implicitly, desHeaderCarrier, ec)
    }
  }
}
