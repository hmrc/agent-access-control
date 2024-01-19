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

package uk.gov.hmrc.agentaccesscontrol.config

import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.agentaccesscontrol.mocks.config.MockAppConfig

class AppConfigSpec extends UnitSpec {

  private val appConfig = MockAppConfig.mockAppConfig

  "getConfString" should {
    "return a value" when {
      "it has been set in config" in {
        appConfig.getConfString("des.authorization-token") mustBe "secret"
      }
    }

    "throw an exception" when {
      "the value has not been set in config" in {
        an[RuntimeException] shouldBe thrownBy(
          appConfig.getConfString("key-does-not-exist")
        )
      }
    }
  }

}
