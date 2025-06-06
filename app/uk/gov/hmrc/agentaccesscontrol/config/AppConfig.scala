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

package uk.gov.hmrc.agentaccesscontrol.config

import com.google.inject.Inject
import com.google.inject.Singleton
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (servicesConfig: ServicesConfig) {

  val appName = "agent-access-control"

  def getConfString(key: String): String =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config '$key' not found"))

  def baseUrl(serviceName: String): String = servicesConfig.baseUrl(serviceName)

  lazy val authBaseUrl: String = baseUrl("auth")

  lazy val acrBaseUrl: String = baseUrl("agent-client-relationships")

  lazy val agentPermissionsUrl: String =
    servicesConfig.baseUrl("agent-permissions")

  lazy val desUrl: String = baseUrl("des")

  lazy val desToken: String = getConfString("des.authorization-token")

  lazy val desEnv: String = getConfString("des.environment")

  lazy val desPayeUrl: String = baseUrl("des-paye")

  lazy val desSAUrl: String = baseUrl("des-sa")

  lazy val esProxyBaseUrl: String = baseUrl("enrolment-store-proxy")

  lazy val afiBaseUrl: String = baseUrl("agent-fi-relationship")

  lazy val agentMappingBaseUrl: String = baseUrl("agent-mapping")

  lazy val agentAssuranceBaseUrl: String = baseUrl("agent-assurance")

  def enableGranularPermissions: Boolean =
    servicesConfig.getBoolean("features.enable-granular-permissions")
}
