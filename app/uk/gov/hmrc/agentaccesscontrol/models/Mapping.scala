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

package uk.gov.hmrc.agentaccesscontrol.models

import play.api.libs.json.Format
import play.api.libs.json.Json.format

case class AgentReferenceMappings(mappings: List[AgentReferenceMapping])

object AgentReferenceMappings {
  implicit val formats: Format[AgentReferenceMappings] =
    format[AgentReferenceMappings]
}

trait ArnToIdentifierMapping {
  def arn: String
  def identifier: String
}

case class AgentReferenceMapping(arn: String, identifier: String) extends ArnToIdentifierMapping

object AgentReferenceMapping {
  implicit val formats: Format[AgentReferenceMapping] =
    format[AgentReferenceMapping]
}
