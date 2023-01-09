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

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AgentPermissionsConnectorImpl])
trait AgentPermissionsConnector {
  def granularPermissionsOptinRecordExists(arn: Arn)(
      implicit hc: HeaderCarrier,
      executionContext: ExecutionContext): Future[Boolean]
}

@Singleton
class AgentPermissionsConnectorImpl @Inject()(
    http: HttpClient,
    metrics: Metrics)(implicit appConfig: AppConfig)
    extends AgentPermissionsConnector
    with HttpAPIMonitor
    with Logging {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val agentPermissionsBaseUrl = new URL(appConfig.agentPermissionsUrl)

  def granularPermissionsOptinRecordExists(arn: Arn)(
      implicit hc: HeaderCarrier,
      executionContext: ExecutionContext): Future[Boolean] = {
    val url =
      new URL(agentPermissionsBaseUrl,
              s"/agent-permissions/arn/${arn.value}/optin-record-exists")
    monitor(s"ConsumedAPI-AP-granularPermissionsOptinRecordExists-GET") {
      http.GET[HttpResponse](url.toString).map { response =>
        response.status match {
          case Status.NO_CONTENT => true
          case Status.NOT_FOUND  => false
          case other =>
            logger.warn(
              s"Got $other when checking for optin record exists. Response message: '${response.body}''")
            false
        }
      }
    }
  }
}
