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

package uk.gov.hmrc.agentaccesscontrol.model

import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaAgentReference

import scala.concurrent.Future

case class EnrolmentIdentifier(key: String, value: String)
case class AuthEnrolment(key: String, identifiers: Seq[EnrolmentIdentifier], state: String) {
  val isActivated: Boolean = state == "Activated"
  def identifier: Option[String] = identifiers.headOption.map(_.value)
}

object AuthEnrolment {
  implicit val idformat = Json.format[EnrolmentIdentifier]
  implicit val format = Json.format[AuthEnrolment]
}

case class Enrolments(enrolments: Set[AuthEnrolment]) {

  def saAgentReferenceOption: Option[SaAgentReference] = saEnrolment.flatMap(_.identifier).map(SaAgentReference)

  private def saEnrolment: Option[AuthEnrolment] = getActivatedEnrolment("IR-SA-AGENT")

  private def getActivatedEnrolment(key: String): Option[AuthEnrolment] = enrolments.find(e => e.key == key && e.isActivated)
}

object Enrolments {
  implicit val formats = Json.format[Enrolments]
}

