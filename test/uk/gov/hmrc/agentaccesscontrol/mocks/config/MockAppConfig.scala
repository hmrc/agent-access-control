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

package uk.gov.hmrc.agentaccesscontrol.mocks.config

import play.api.Configuration
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

object MockAppConfig {

  private val configuration: Map[String, Any] = Map(
    "microservice.services.auth.host" -> "localhost",
    "microservice.services.auth.port" -> "0000",
    "microservice.services.agent-client-relationships.host" -> "localhost",
    "microservice.services.agent-client-relationships.port" -> "0000",
    "microservice.services.agent-permissions.host" -> "localhost",
    "microservice.services.agent-permissions.port" -> "0000",
    "microservice.services.des.host" -> "localhost",
    "microservice.services.des.port" -> "0000",
    "microservice.services.des.authorization-token" -> "secret",
    "microservice.services.des.environment" -> "local",
    "microservice.services.des-paye.host" -> "localhost",
    "microservice.services.des-paye.port" -> "0000",
    "microservice.services.des-sa.host" -> "localhost",
    "microservice.services.des-sa.port" -> "0000",
    "microservice.services.enrolment-store-proxy.host" -> "localhost",
    "microservice.services.enrolment-store-proxy.port" -> "0000",
    "microservice.services.agent-mapping.host" -> "localhost",
    "microservice.services.agent-mapping.port" -> "0000",
    "microservice.services.agent-fi-relationship.host" -> "localhost",
    "microservice.services.agent-fi-relationship.port" -> "0000",
    "microservice.services.agent-client-authorisation.host" -> "localhost",
    "microservice.services.agent-client-authorisation.port" -> "0000",
    "features.enable-granular-permissions" -> true,
    "auditing.enabled" -> false
  )

  val mockAppConfig: AppConfig =
    new AppConfig(new ServicesConfig(Configuration.from(configuration)))

}
