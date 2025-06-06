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

package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.domain.AgentUserId
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

@Singleton
class EnrolmentStoreProxyConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2, metrics: Metrics) {
  private def pathES0(enrolmentKey: String, usersType: String): URL =
    url"${appConfig.esProxyBaseUrl}/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=$usersType"

  def getIRSAAGENTPrincipalUserIdsFor(
      saAgentReference: SaAgentReference
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[AgentUserId]] =
    getES0(s"IR-SA-AGENT~IRAgentReference~${saAgentReference.value}", "principal")

  def getIRSADelegatedUserIdsFor(
      utr: SaUtr
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[AgentUserId]] =
    getES0(s"IR-SA~UTR~$utr", "delegated")

  def getIRPAYEDelegatedUserIdsFor(
      empRef: EmpRef
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[AgentUserId]] = {
    val enrolmentKey =
      s"IR-PAYE~TaxOfficeNumber~${empRef.taxOfficeNumber}~TaxOfficeReference~${empRef.taxOfficeReference}"

    getES0(enrolmentKey, "delegated")
  }

  private def getES0(
      enrolmentKey: String,
      usersType: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[AgentUserId]] = {

    val url = pathES0(enrolmentKey, usersType)

    val timer =
      metrics.defaultRegistry.timer("Timer-ConsumedAPI-EnrolmentStoreProxy-ES0-GET")

    timer.time()
    httpClient.get(url).execute[HttpResponse].map { response =>
      timer.time().stop()
      response.status match {
        case OK =>
          usersType match {
            case "delegated" => parseResponseDelegated(response.json)
            case "principal" => parseResponsePrincipal(response.json)
          }
        case NO_CONTENT | BAD_REQUEST => Set.empty[AgentUserId]
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

  private def parseResponseDelegated(json: JsValue): Set[AgentUserId] =
    (json \ "delegatedUserIds").as[Set[String]].map(AgentUserId)

  private def parseResponsePrincipal(json: JsValue): Set[AgentUserId] =
    (json \ "principalUserIds").as[Set[String]].map(AgentUserId)
}
