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

import com.google.inject.ImplementedBy
import play.api.http.Status
import play.api.libs.json.Json
import play.api.Logging
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.models.accessgroups.TaxGroup
import uk.gov.hmrc.agentaccesscontrol.models.Arn
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse

@ImplementedBy(classOf[AgentPermissionsConnectorImpl])
trait AgentPermissionsConnector {
  def granularPermissionsOptinRecordExists(
      arn: Arn
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Boolean]

  def getTaxServiceGroups(arn: Arn, service: String)(
      implicit hc: HeaderCarrier,
      executionContext: ExecutionContext
  ): Future[Option[TaxGroup]]
}

@Singleton
class AgentPermissionsConnectorImpl @Inject() (http: HttpClientV2)(implicit appConfig: AppConfig)
    extends AgentPermissionsConnector
    with Logging {

  val agentPermissionsBaseUrl = new URL(appConfig.agentPermissionsUrl)

  def granularPermissionsOptinRecordExists(
      arn: Arn
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Boolean] = {

    val url = url"$agentPermissionsBaseUrl/agent-permissions/arn/${arn.value}/optin-record-exists"

    http.get(url).execute[HttpResponse].map { response =>
      response.status match {
        case Status.NO_CONTENT => true
        case Status.NOT_FOUND  => false
        case _ =>
          logger
            .warn(s"Got ${response.status} when checking for optin record exists. Response message: '${response.body}'")
          false
      }
    }
  }

  def getTaxServiceGroups(
      arn: Arn,
      service: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Option[TaxGroup]] = {

    val url = url"$agentPermissionsBaseUrl/agent-permissions/arn/${arn.value}/tax-group/$service"

    http.get(url).execute[HttpResponse].map { response =>
      response.status match {
        case Status.NOT_FOUND => None
        case Status.OK =>
          Json
            .parse(response.body)
            .asOpt[TaxGroup]
            .orElse(
              throw new RuntimeException(
                s"getTaxServiceGroups returned invalid Json for $arn $service: ${response.body}"
              )
            )
        case _ =>
          throw UpstreamErrorResponse(
            s"getTaxServiceGroups returned unexpected response ${response.status}",
            response.status
          )
      }
    }
  }
}
