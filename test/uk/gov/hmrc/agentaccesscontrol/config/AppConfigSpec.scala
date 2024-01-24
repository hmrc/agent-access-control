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

import play.api.Configuration
import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends UnitSpec {

  trait Setup {
    private val configuration: Map[String, Any] = Map(
      "microservice.services.des.authorization-token" -> "secret",
      "microservice.services.auth.host" -> "localhost",
      "microservice.services.auth.port" -> "0000",
      "features.enable-granular-permissions" -> true
    )
    val stubbedAppConfig: AppConfig = new AppConfig(
      new ServicesConfig(Configuration.from(configuration)))

    val emptyAppConfig: AppConfig = new AppConfig(
      new ServicesConfig(Configuration.from(Map.empty)))
  }

  "baseUrl" should {
    "return a value" when {
      "it has been set in config" in new Setup {
        stubbedAppConfig.baseUrl("auth") mustBe "http://localhost:0"
      }
    }

    "throw an exception" when {
      "the value has not been set in config" in new Setup {
        an[RuntimeException] shouldBe thrownBy(
          emptyAppConfig.baseUrl("serviceName-does-not-exist")
        )
      }
    }
  }

  "enableGranularPermissions" should {
    "return a value" when {
      "it has been set in config" in new Setup {
        stubbedAppConfig.enableGranularPermissions mustBe true
      }
    }

    "throw an exception" when {
      "the value has not been set in config" in new Setup {
        an[RuntimeException] shouldBe thrownBy(
          emptyAppConfig.enableGranularPermissions
        )
      }
    }
  }

  "getConfString" should {
    "return a value" when {
      "it has been set in config" in new Setup {
        stubbedAppConfig.getConfString("des.authorization-token") mustBe "secret"
      }
    }

    "throw an exception" when {
      "the value has not been set in config" in new Setup {
        an[RuntimeException] shouldBe thrownBy(
          emptyAppConfig.getConfString("key-does-not-exist")
        )
      }
    }
  }

}
