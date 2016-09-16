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
import uk.gov.hmrc.agentaccesscontrol.connectors.{GovernmentGatewayProxyConnector, AuthConnector => OurAuthConnector}
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.controllers.AuthorisationController
import uk.gov.hmrc.agentaccesscontrol.service.{AuthorisationService, CesaAuthorisationService, GovernmentGatewayAuthorisationService}
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName with MonitoredWSHttp {
  val httpAPIs = Map(".*/sa/agents/\\w+/client/\\w+" -> "DES-GetAgentClientRelationship",
                     ".*/auth/authority" -> "AUTH-GetAuthority")

  override val hooks: Seq[HttpHook] = NoneRequired
}

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}


trait ServiceRegistry extends ServicesConfig {
  lazy val auditService: AuditService.type = AuditService
  lazy val desAgentClientApiConnector = {
    val desAuthToken = getConfString("des.authorization-token", throw new RuntimeException("Could not find DES authorisation token"))
    val desEnvironment = getConfString("des.environment", throw new RuntimeException("Could not find DES environment"))
    new DesAgentClientApiConnector(baseUrl("des"), desAuthToken, desEnvironment, WSHttp, auditService)
  }
  lazy val authConnector = new OurAuthConnector(new URL(baseUrl("auth")), WSHttp)
  lazy val cesaAuthorisationService = new CesaAuthorisationService(desAgentClientApiConnector, auditService)
  lazy val ggProxyConnector: GovernmentGatewayProxyConnector =
    new GovernmentGatewayProxyConnector(new URL(baseUrl("government-gateway-proxy")), WSHttp, auditService)
  lazy val ggAuthorisationService = new GovernmentGatewayAuthorisationService(ggProxyConnector, auditService)
  lazy val authorisationService: AuthorisationService =
    new AuthorisationService(cesaAuthorisationService, authConnector, ggAuthorisationService, auditService)
}

trait ControllerRegistry {
  registry: ServiceRegistry =>

  private lazy val controllers = Map[Class[_], Controller](
    classOf[AuthorisationController] -> new AuthorisationController(auditService, authorisationService)
  )

  def getController[A](controllerClass: Class[A]) : A = controllers(controllerClass).asInstanceOf[A]
}
