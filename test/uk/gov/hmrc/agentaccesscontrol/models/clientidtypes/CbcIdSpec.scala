/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.models.clientidtypes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CbcIdSpec extends AnyWordSpec with Matchers {

  "validation" should {
    "pass for a valid PPT" in {
      CbcId.isValid("XACBC0123456789") shouldBe true
    }

    "fail when it has more than 15 characters" in {
      CbcId.isValid("XACBC01234567890") shouldBe false
    }

    "fail when it has less than 15 characters" in {
      CbcId.isValid("XACBC012345678") shouldBe false
    }

    "fail when it is empty" in {
      CbcId.isValid("") shouldBe false
    }

    "fail when it contains lowercase characters" in {
      CbcId.isValid("xacbc0123456789") shouldBe false
    }

    "fail when it has non-alphanumeric characters" in {
      CbcId.isValid("XACBC012345678!") shouldBe false
    }
  }
}
