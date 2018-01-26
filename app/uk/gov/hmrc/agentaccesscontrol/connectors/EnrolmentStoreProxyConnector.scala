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

package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import com.kenshoo.play.metrics.Metrics
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.domain.{EmpRef, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

case class AssignedAgentCredentials(userId: String)

@Singleton
class EnrolmentStoreProxyConnector @Inject() (@Named("enrolment-store-proxy-baseUrl") baseUrl: URL, httpGet: HttpGet, metrics: Metrics)
      extends HttpAPIMonitor {
  override val kenshooRegistry = metrics.defaultRegistry

  private def pathES0(enrolmentKey: String): String = {
    new URL(baseUrl, s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=delegated").toString
  }

  def assignedSaAgents(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Seq[AssignedAgentCredentials]] = {
    getES0(s"IR-SA~UTR~$utr")
  }

  def assignedPayeAgents(empRef: EmpRef)(implicit hc: HeaderCarrier): Future[Seq[AssignedAgentCredentials]] = {
    val enrolmentKey = s"IR-PAYE~TaxOfficeNumber~${empRef.taxOfficeNumber}~TaxOfficeReference~${empRef.taxOfficeReference}"

    getES0(enrolmentKey)
  }

  private def getES0(enrolmentKey: String)(implicit hc: HeaderCarrier): Future[Seq[AssignedAgentCredentials]] = {
    monitor("ConsumedAPI-EnrolmentStoreProxy-ES0-GET") {
      httpGet.GET(pathES0(enrolmentKey), Seq(CONTENT_TYPE -> JSON))
    }.map(parseResponse)
  }

  private def parseResponse(response: HttpResponse): Seq[AssignedAgentCredentials] = {
    (response.json \ "delegatedUserIds").as[List[AssignedAgentCredentials]]
  }
}