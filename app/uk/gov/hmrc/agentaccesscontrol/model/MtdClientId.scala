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

import uk.gov.hmrc.domain.{SimpleObjectReads, SimpleObjectWrites}

case class MtdClientId(value: String)
object MtdClientId{
  implicit val reads = new SimpleObjectReads[MtdClientId]("clientId", MtdClientId.apply)
  implicit val writes = new SimpleObjectWrites[MtdClientId](_.value)
}

case class Arn(value: String)
object Arn {
  implicit val reads = new SimpleObjectReads[Arn]("arn", Arn.apply)
  implicit val writes = new SimpleObjectWrites[Arn](_.value)
}

