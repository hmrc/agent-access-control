/*
 * Copyright 2022 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.domain.{AgentUserId, EmpRef, SaAgentReference, SaUtr}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpResponse,
  UpstreamErrorResponse
}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(appConfig: AppConfig,
                                             httpClient: HttpClient,
                                             metrics: Metrics)
    extends HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private def pathES0(enrolmentKey: String, usersType: String): String =
    new URL(
      s"${appConfig.esProxyBaseUrl}/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=$usersType").toString

  def getIRSAAGENTPrincipalUserIdsFor(saAgentReference: SaAgentReference)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Set[AgentUserId]] =
    getES0(s"IR-SA-AGENT~IRAgentReference~${saAgentReference.value}",
           "principal")

  def getIRSADelegatedUserIdsFor(utr: SaUtr)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Set[AgentUserId]] =
    getES0(s"IR-SA~UTR~$utr", "delegated")

  def getIRPAYEDelegatedUserIdsFor(empRef: EmpRef)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Set[AgentUserId]] = {
    val enrolmentKey =
      s"IR-PAYE~TaxOfficeNumber~${empRef.taxOfficeNumber}~TaxOfficeReference~${empRef.taxOfficeReference}"
    getES0(enrolmentKey, "delegated")
  }

  private def getES0(enrolmentKey: String, usersType: String)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Set[AgentUserId]] = {
    val url = pathES0(enrolmentKey, usersType)
    monitor("ConsumedAPI-EnrolmentStoreProxy-ES0-GET") {
      httpClient.GET[HttpResponse](url)
    }.map(response =>
      response.status match {
        case OK =>
          usersType match {
            case "delegated" => parseResponseDelegated(response.json)
            case "principal" => parseResponsePrincipal(response.json)
          }
        case NO_CONTENT | BAD_REQUEST => Set.empty[AgentUserId]
        case s =>
          throw UpstreamErrorResponse(
            s"Error calling in getSaAgentClientRelationship at: $url",
            s,
            if (s == INTERNAL_SERVER_ERROR) BAD_GATEWAY else s)
    })
  }

  private def parseResponseDelegated(json: JsValue): Set[AgentUserId] =
    (json \ "delegatedUserIds").as[Set[String]].map(AgentUserId)

  private def parseResponsePrincipal(json: JsValue): Set[AgentUserId] =
    (json \ "principalUserIds").as[Set[String]].map(AgentUserId)
}
