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

import java.net.URL
import java.util.Base64
import javax.inject.{Inject, Provider, Singleton}

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.kenshoo.play.metrics.Metrics
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import org.slf4j.MDC
import play.api._
import play.api.http.HttpFilters
import play.api.mvc.{Call, EssentialFilter}
import uk.gov.hmrc.agent.kenshoo.monitoring.MonitoringFilter
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.audit.http.config.ErrorAuditingSettings
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, ServicesConfig}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.graphite.GraphiteConfig
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.microservice.bootstrap.JsonErrorHandling
import uk.gov.hmrc.play.microservice.bootstrap.Routing.RemovingOfTrailingSlashes
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter


class GuiceModule() extends AbstractModule with ServicesConfig {
  def configure() = {
    bind(classOf[HttpGet]).toInstance(WSHttp)
    bind(classOf[HttpPut]).toInstance(WSHttp)
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[AuditConnector]).toInstance(MicroserviceGlobal.auditConnector)
    bindBaseUrl("auth")
    bindBaseUrl("agent-client-relationships")
    bindBaseUrl("des")
    bindBaseUrl("government-gateway-proxy")
    bindConfigProperty("des.authorization-token")
    bindConfigProperty("des.environment")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindConfigProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(s"$propertyName")).toProvider(new ConfigPropertyProvider(propertyName))

  private class ConfigPropertyProvider(propertyName: String) extends Provider[String] {
    override lazy val get = getConfString(propertyName, throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

}



object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceGlobal.auditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

@Singleton
class MicroserviceMonitoringFilter @Inject() (metrics: Metrics)
  extends MonitoringFilter(Map(".*/sa-auth/agent/\\w+/client/\\w+" -> "Agent-SA-Access-Control",
                               ".*/epaye-auth/agent/\\w+/client/[\\w%]+" -> "Agent-PAYE-Access-Control",
                               ".*/mtd-it-auth/agent/\\w+/client/\\w+" -> "Agent-MTD-IT-Access-Control"),
                           metrics.defaultRegistry) with MicroserviceFilterSupport

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = MicroserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

@Singleton
class WhitelistFilter @Inject() (configuration: Configuration) extends AkamaiWhitelistFilter with MicroserviceFilterSupport {

  override val whitelist: Seq[String] = whitelistConfig("microservice.whitelist.ips")
  override val destination: Call = Call("GET", "/agent-access-control/forbidden")
  override val excludedPaths: Seq[Call] = Seq(
    Call("GET", "/ping/ping"),
    Call("GET", "/admin/details"),
    Call("GET", "/admin/metrics"),
    Call("GET", "/agent-access-control/forbidden")
  )

  def enabled(): Boolean = configuration.getBoolean("microservice.whitelist.enabled").getOrElse(true)

  private def whitelistConfig(key: String): Seq[String] =
    new String(Base64.getDecoder().decode(configuration.getString(key).getOrElse("")), "UTF-8").split(",")
}

class Filters @Inject() (whitelistFilter: WhitelistFilter,
                         monitoringFilter: MicroserviceMonitoringFilter) extends HttpFilters {

  private lazy val whitelistFilterSeq = if (whitelistFilter.enabled()) {
    Logger.info("Starting microservice with IP whitelist enabled")
    Seq(whitelistFilter.asInstanceOf[EssentialFilter])
  } else {
    Logger.info("Starting microservice with IP whitelist disabled")
    Seq.empty
  }

  override def filters = whitelistFilterSeq ++ Seq(
    monitoringFilter,
    MicroserviceAuditFilter,
    MicroserviceLoggingFilter,
    MicroserviceAuthFilter)
}

object MicroserviceGlobal
  extends GlobalSettings
    with GraphiteConfig
    with RemovingOfTrailingSlashes
    with JsonErrorHandling
    with ErrorAuditingSettings {

  lazy val appName = Play.current.configuration.getString("appName").getOrElse("APP NAME NOT SET")
  lazy val loggerDateFormat: Option[String] = Play.current.configuration.getString("logger.json.dateformat")

  override def onStart(app: Application) {
    Logger.info(s"Starting microservice : $appName : in mode : ${app.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))
    super.onStart(app)
  }

  override val auditConnector = new MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
}
