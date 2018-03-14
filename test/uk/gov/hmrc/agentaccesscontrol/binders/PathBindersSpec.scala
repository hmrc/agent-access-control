/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.binders

import uk.gov.hmrc.play.test.UnitSpec

class EmpRefBinderSpec extends UnitSpec {

  "bind" should {
    "parse an empref" in {
      val result = PathBinders.EmpRefBinder.bind("foo", "123/02345677").right.get

      result.taxOfficeNumber shouldBe "123"
      result.taxOfficeReference shouldBe "02345677"
    }

    "not parse an invalid string" in {
      val result = PathBinders.EmpRefBinder.bind("foo", "not_an_empref").left.get

      result shouldBe "Cannot parse parameter 'foo' with value 'not_an_empref' as EmpRef"
    }
  }
}
