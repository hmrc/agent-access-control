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

package uk.gov.hmrc.agentaccesscontrol.config

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig) {

  val appName = "agent-access-control"

  def getConfString(key: String) =
    servicesConfig.getConfString(
      key,
      throw new RuntimeException(s"config '$key' not found"))

  def baseUrl(serviceName: String) = servicesConfig.baseUrl(serviceName)

  val authBaseUrl = baseUrl("auth")

  val acrBaseUrl = baseUrl("agent-client-relationships")

  val desUrl = baseUrl("des")

  val desToken = getConfString("des.authorization-token")

  val desEnv = getConfString("des.environment")

  val desPayeUrl = baseUrl("des-paye")

  val desSAUrl = baseUrl("des-sa")

  val esProxyBaseUrl = baseUrl("enrolment-store-proxy")

  val afiBaseUrl = baseUrl("agent-fi-relationship")

  val agentMappingBaseUrl = baseUrl("agent-mapping")

  val featureAgentSuspension =
    servicesConfig.getBoolean("features.enable-agent-suspension")

  val featuresPayeAccess = servicesConfig.getBoolean("features.allowPayeAccess")
}
