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

package uk.gov.hmrc.agentaccesscontrol.connectors.desapi

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.model._
import uk.gov.hmrc.domain._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpReads, NotFoundException}
import uk.gov.hmrc.http.logging.Authorization

@Singleton
class DesAgentClientApiConnector @Inject() (@Named("des-baseUrl") desBaseUrl: URL,
                                            @Named("des.authorization-token") authorizationToken: String,
                                            @Named("des.environment") environment: String,
                                            httpGet: HttpGet,
                                            metrics: Metrics)
  extends HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private implicit val foundResponseReads: Reads[SaFoundResponse] = (
    (__ \ "Auth_64-8").read[Boolean] and
    (__ \ "Auth_i64-8").read[Boolean]
    ) (SaFoundResponse)

  private implicit val payeFoundResponseReads: Reads[PayeFoundResponse] =
    (__ \ "Auth_64-8").read[Boolean].map(PayeFoundResponse.apply)


  def getSaAgentClientRelationship(saAgentReference: SaAgentReference, saUtr: SaUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext):
        Future[SaDesAgentClientFlagsApiResponse] = {
    val url = saUrlFor(saAgentReference, saUtr)
    getWithDesHeaders("GetSaAgentClientRelationship",url) map { r =>
      foundResponseReads.reads(Json.parse(r.body)).get
    } recover {
      case _: NotFoundException => SaNotFoundResponse
    }
  }

  def getPayeAgentClientRelationship(agentCode: AgentCode, empRef: EmpRef)(implicit hc: HeaderCarrier, ec: ExecutionContext):
        Future[PayeDesAgentClientFlagsApiResponse] = {
    val url = payeUrlFor(agentCode, empRef)
    getWithDesHeaders("GetPayeAgentClientRelationship",url) map { r =>
      payeFoundResponseReads.reads(Json.parse(r.body)).get
    } recover {
      case _: NotFoundException => PayeNotFoundResponse
    }
  }

  private def saUrlFor(saAgentReference: SaAgentReference, saUtr: SaUtr): URL =
    new URL(desBaseUrl, s"/sa/agents/${saAgentReference.value}/client/$saUtr")

  private def payeUrlFor(agentCode: AgentCode, empRef: EmpRef): URL =
    new URL(desBaseUrl, s"/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}")

  private def getWithDesHeaders[A: HttpReads](apiName: String, url: URL)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ "Environment" -> environment)
    monitor(s"ConsumedAPI-DES-$apiName-GET") {
      httpGet.GET[A](url.toString)(implicitly[HttpReads[A]], desHeaderCarrier, ec)
    }
  }
}
