/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Named, Singleton}
import play.api.Configuration
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.{CredentialRole, PlayAuthConnector}
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HttpPost
import uk.gov.hmrc.play.http.ws.WSPost

@Singleton
class AgentAccessAuthConnector @Inject()(@Named("auth-baseUrl") baseUrl: URL,
                                         val config: Configuration,
                                         val _actorSystem: ActorSystem)
    extends PlayAuthConnector {

  override val serviceUrl = baseUrl.toString

  override def http = new HttpPost with WSPost {
    override val hooks = NoneRequired
    override protected def configuration: Option[Config] =
      Some(config.underlying)
    override protected def actorSystem: ActorSystem = _actorSystem
  }
}

case class AuthDetails(saAgentReference: Option[SaAgentReference],
                       arn: Option[Arn],
                       ggCredentialId: String,
                       affinityGroup: Option[String],
                       agentUserRole: Option[CredentialRole])
