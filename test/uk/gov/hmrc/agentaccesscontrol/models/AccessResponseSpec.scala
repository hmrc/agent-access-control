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

import uk.gov.hmrc.agentaccesscontrol.helpers.UnitTest
import uk.gov.hmrc.agentaccesscontrol.models.AccessResponse.{
  AgentSuspended,
  Authorised,
  NoAssignment,
  NoRelationship,
  Error,
  toReason
}

class AccessResponseSpec extends UnitTest {

  "AccessResponse.toReason" should {
    "convert an 'Authorised' AccessResponse to no reason" in {
      toReason(Authorised) shouldBe Seq.empty
    }
    "convert a 'NoRelationship' AccessResponse to a 'NoRelationship' reason" in {
      toReason(NoRelationship) shouldBe Seq(("reason", "NoRelationship"))
    }
    "convert a 'NoAssignment' AccessResponse to a 'NoAssignment' reason" in {
      toReason(NoAssignment) shouldBe Seq(("reason", "NoAssignment"))
    }
    "convert an 'AgentSuspended' AccessResponse to a 'NoRelationship' reason" in {
      toReason(AgentSuspended) shouldBe Seq(("reason", "NoRelationship"))
    }
    "convert an 'Error' AccessResponse to a 'NoRelationship' reason" in {
      toReason(Error("error")) shouldBe Seq(("reason", "NoRelationship"))
    }
  }

}
