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

package uk.gov.hmrc.agentaccesscontrol.controllers

import uk.gov.hmrc.auth.core.Enrolment

object EnrolmentHelper {

  val AsAgentServiceKey = "HMRC-AS-AGENT" // The main Enrolment for MTD Agent Services
  val IRSAServiceKey = "IR-SA-AGENT" // Used for ATED; we don't map this, as it uses the same ARN as above

  val ArnEnrolmentKey = "AgentReferenceNumber"

  def userHasAsAgentEnrolment(userEnrolments: Set[Enrolment]): Boolean =
    containsKey(userEnrolments, AsAgentServiceKey)

  def userHasIRSAAgentEnrolment(userEnrolments: Set[Enrolment]): Boolean =
    containsKey(userEnrolments, IRSAServiceKey)

  private def containsKey(enrolments: Set[Enrolment], key: String): Boolean =
    enrolments.map(_.key).contains(key)

}
