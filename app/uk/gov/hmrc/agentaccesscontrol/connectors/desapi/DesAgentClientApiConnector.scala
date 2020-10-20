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

package uk.gov.hmrc.agentaccesscontrol.connectors.desapi

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.utils.UriEncoding
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http._

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

  def getAgentRecord(agentId: TaxIdentifier)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Either[String, AgentRecord]]

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
        case OK        => foundResponseReads.reads(Json.parse(r.body)).get
        case NOT_FOUND => SaNotFoundResponse
        case INTERNAL_SERVER_ERROR =>
          throw UpstreamErrorResponse(
            s"Error calling in getSaAgentClientRelationship at: $url",
            BAD_GATEWAY)
        case s =>
          throw UpstreamErrorResponse(
            s"Error calling in getSaAgentClientRelationship at: $url",
            s)
      }
    }

  }

  def getPayeAgentClientRelationship(agentCode: AgentCode, empRef: EmpRef)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[PayeDesAgentClientFlagsApiResponse] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
    val url = payeUrlFor(agentCode, empRef)
    getWithDesHeaders("GetPayeAgentClientRelationship", url) map (r =>
      r.status match {
        case OK        => payeFoundResponseReads.reads(Json.parse(r.body)).get
        case NOT_FOUND => PayeNotFoundResponse
        case INTERNAL_SERVER_ERROR =>
          throw UpstreamErrorResponse(
            s"Error calling in getSaAgentClientRelationship at: $url",
            BAD_GATEWAY)
        case s => throw UpstreamErrorResponse(s"Error calling: $url", s)
      })
  }

  private def saUrlFor(saAgentReference: SaAgentReference, saUtr: SaUtr): URL =
    new URL(s"$desBaseUrlSa/sa/agents/${saAgentReference.value}/client/$saUtr")

  private def payeUrlFor(agentCode: AgentCode, empRef: EmpRef): URL =
    new URL(
      s"$desBaseUrlPaye/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}")

  def getAgentRecord(agentId: TaxIdentifier)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Either[String, AgentRecord]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._
    getWithDesHeaders[AgentRecord]("GetAgentRecord",
                                   new URL(getAgentRecordUrl(agentId)))
      .map(a => Right(a))
      .recover {
        case e: Upstream4xxResponse
            if e.message contains ("AGENT_TERMINATED") =>
          Left(e.message)
      }
  }

  private def getAgentRecordUrl(agentId: TaxIdentifier) =
    agentId match {
      case Arn(arn) =>
        val encodedArn = UriEncoding.encodePathSegment(arn, "UTF-8")
        s"$desBaseUrl/registration/personal-details/arn/$encodedArn"
      case Utr(utr) =>
        val encodedUtr = UriEncoding.encodePathSegment(utr, "UTF-8")
        s"$desBaseUrl/registration/personal-details/utr/$encodedUtr"
      case _ =>
        throw new Exception(s"The client identifier $agentId is not supported.")
    }

  private def getWithDesHeaders[A: HttpReads](apiName: String, url: URL)(
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
