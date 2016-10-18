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

package uk.gov.hmrc.agentaccesscontrol.service

import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.{AgenciesConnector, RelationshipsConnector}
import uk.gov.hmrc.agentaccesscontrol.model.{Arn, MtdSaClientId}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MtdAuthorisationService(agenciesConnector: AgenciesConnector,
                              relationshipsConnector: RelationshipsConnector) {

  def authoriseForSa(agentCode: AgentCode, mtdSaClientId: MtdSaClientId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    agenciesConnector.fetchAgencyRecord(agentCode) flatMap  {
      case Some(agency) => hasRelationship(agency.arn, mtdSaClientId)
      case None => Future successful false
    }
  }

  private def hasRelationship(arn: Arn, mtdSaClientId: MtdSaClientId)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    relationshipsConnector.fetchRelationship(arn, mtdSaClientId) map { _.isDefined }
  }
}
