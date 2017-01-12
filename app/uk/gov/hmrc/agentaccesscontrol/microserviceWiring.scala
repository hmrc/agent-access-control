/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.Play
import uk.gov.hmrc.agent.kenshoo.monitoring.MonitoredWSHttp
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

trait WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName with MonitoredWSHttp with HttpAuditing {
  val httpAPIs = Map(".*/sa/agents/\\w+/client/\\w+" -> "DES-GetSaAgentClientRelationship",
                     ".*/agents/regime/PAYE/agent/\\w+/client/\\w+" -> "DES-GetPayeAgentClientRelationship",
                     ".*/auth/authority" -> "AUTH-GetAuthority",
                     ".*/agencies/agentcode/\\w+" -> "AGENCIES-GetAgencyByAgentCode",
                     ".*/relationships/mtd-sa/.*" -> "RELATIONSHIPS-GetAgentClientRelationship")

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override lazy val kenshooRegistry: MetricRegistry = Play.current.injector.instanceOf[Metrics].defaultRegistry
}

object WSHttp extends WSHttp {
  override val auditConnector = MicroserviceGlobal.auditConnector
}

class MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}
