/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.URL

import play.api.mvc.Controller
import uk.gov.hmrc.agent.kenshoo.monitoring.MonitoredWSHttp
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.{AgenciesConnector, RelationshipsConnector}
import uk.gov.hmrc.agentaccesscontrol.connectors.{GovernmentGatewayProxyConnector, AuthConnector => OurAuthConnector}
import uk.gov.hmrc.agentaccesscontrol.controllers.{AuthorisationController, WhitelistController}
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, DesAuthorisationService, GovernmentGatewayAuthorisationService, MtdAuthorisationService}
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


trait ServiceRegistry extends ServicesConfig {
  lazy val auditConnector: AuditConnector = new MicroserviceAuditConnector
  lazy val auditService = new AuditService(auditConnector)
  lazy val desAgentClientApiConnector = {
    val desAuthToken = getConfString("des.authorization-token", throw new RuntimeException("Could not find DES authorisation token"))
    val desEnvironment = getConfString("des.environment", throw new RuntimeException("Could not find DES environment"))
    new DesAgentClientApiConnector(baseUrl("des"), desAuthToken, desEnvironment, WSHttp)
  }
  lazy val authConnector = new OurAuthConnector(new URL(baseUrl("auth")), WSHttp)
  lazy val desAuthorisationService = new DesAuthorisationService(desAgentClientApiConnector)
  lazy val ggProxyConnector: GovernmentGatewayProxyConnector =
    new GovernmentGatewayProxyConnector(new URL(baseUrl("government-gateway-proxy")), WSHttp)
  lazy val ggAuthorisationService = new GovernmentGatewayAuthorisationService(ggProxyConnector)
  lazy val authorisationService: AuthorisationService =
    new AuthorisationService(desAuthorisationService, authConnector, ggAuthorisationService, auditService)
  lazy val agenciesConnector = new AgenciesConnector(new URL(baseUrl("agencies-fake")), WSHttp)
  lazy val relationshipsConnector = new RelationshipsConnector(new URL(baseUrl("agent-client-relationships")), WSHttp)
  lazy val mtdAuthorisationService = new MtdAuthorisationService(agenciesConnector, relationshipsConnector, auditService)
}

trait ControllerRegistry {
  registry: ServiceRegistry =>

  private lazy val controllers = Map[Class[_], Controller](
    classOf[AuthorisationController] -> new AuthorisationController(auditService, authorisationService, mtdAuthorisationService),
    classOf[WhitelistController] -> new WhitelistController()
  )

  def getController[A](controllerClass: Class[A]) : A = controllers(controllerClass).asInstanceOf[A]
}
