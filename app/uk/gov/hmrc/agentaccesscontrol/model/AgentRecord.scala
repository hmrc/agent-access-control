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

package uk.gov.hmrc.agentaccesscontrol.model

import play.api.libs.json.{Format, Json}

case class SuspensionDetails(suspensionStatus: Boolean,
                             regimes: Option[Set[String]])

object SuspensionDetails {
  implicit val format: Format[SuspensionDetails] =
    Json.format[SuspensionDetails]
}

case class AgentRecord(suspensionDetails: Option[SuspensionDetails]) {

  def isSuspended: Boolean = suspensionDetails.exists(_.suspensionStatus)

  def suspendedFor(regime: String): Boolean =
    suspensionDetails
      .flatMap(_.regimes)
      .exists(regimes => regimes.contains(regime) || regimes.contains("ALL"))
}

object AgentRecord {
  implicit val format: Format[AgentRecord] = Json.format[AgentRecord]
}
