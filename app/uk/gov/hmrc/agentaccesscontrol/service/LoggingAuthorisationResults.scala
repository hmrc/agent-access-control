/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.service

import play.api.Logging
import uk.gov.hmrc.domain.{AgentCode, TaxIdentifier}

trait LoggingAuthorisationResults extends Logging {

  protected def notAuthorised(agentCode: AgentCode,
                              clientTaxIdentifier: TaxIdentifier,
                              agentUserId: String,
                              agentReference: Option[TaxIdentifier] = None,
                              hasAgents: Option[Boolean] = None): Boolean = {
    logger.info(s"Not authorised: Access not allowed for ${agentReference
      .map(ar => s"agent=$ar")
      .getOrElse("")} agentCode=${agentCode.value} agentUserId=$agentUserId client=$clientTaxIdentifier ${hasAgents
      .map(ha => s"clientHasAgents=$ha")
      .getOrElse("")}")
    false
  }

  protected def notAuthorised(message: String): Boolean = {
    logger.info(s"Not authorised: $message")
    false
  }

  protected def authorised(message: String): Boolean = {
    logger.info(message)
    true
  }

  protected def authorised(
      agentCode: AgentCode,
      clientTaxIdentifier: TaxIdentifier,
      agentUserId: String,
      agentReference: Option[TaxIdentifier] = None): Boolean = {
    logger.info(
      s"Authorised: Access allowed for agent=$agentReference agentCode=${agentCode.value} agentUserId=$agentUserId client=$clientTaxIdentifier")
    true
  }

  protected def found(message: String): Boolean = {
    logger.info(s"Found: $message")
    true
  }

  protected def notFound(message: String): Boolean = {
    logger.info(s"notFound: $message")
    false
  }
}
