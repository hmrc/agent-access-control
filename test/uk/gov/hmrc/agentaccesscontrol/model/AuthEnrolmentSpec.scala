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

import uk.gov.hmrc.play.test.UnitSpec

class AuthEnrolmentSpec extends UnitSpec {

  "isActivated" should {
    "be true when state is \"activated\", regardless of case" in {
      AuthEnrolment("key", Seq.empty, "activated").isActivated shouldBe true
      AuthEnrolment("key", Seq.empty, "Activated").isActivated shouldBe true
      AuthEnrolment("key", Seq.empty, "ACTIVATED").isActivated shouldBe true
    }

    "not be true when state is not \"activated\"" in {
      AuthEnrolment("key", Seq.empty, "pending").isActivated shouldBe false
    }
  }

}
