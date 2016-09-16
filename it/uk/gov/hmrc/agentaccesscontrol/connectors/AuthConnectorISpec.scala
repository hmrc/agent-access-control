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

package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL

import com.kenshoo.play.metrics.MetricsRegistry
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.BaseISpec
import uk.gov.hmrc.domain.SaAgentReference

import scala.concurrent.ExecutionContext.Implicits.global

class AuthConnectorISpec extends BaseISpec {

  "getSaAgentReference" should {
    "return sa agent reference" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasSaAgentReferenceWithEnrolment("REF879")

      await(newAuthConnector().currentAgentIdentifiers) shouldBe Some((Some(SaAgentReference("REF879")), "0000001232456789"))
    }

    "record metrics for both /auth/authority and /auth/authority/(oid)/enrolments" in {
      val metricsRegistry = MetricsRegistry.defaultRegistry
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasSaAgentReferenceWithEnrolment("REF879")

      await(newAuthConnector().currentAgentIdentifiers) shouldBe Some((Some(SaAgentReference("REF879")), "0000001232456789"))
      metricsRegistry.getTimers.get("Timer-ConsumedAPI-AUTH-GetAuthority-GET").getCount should be >= 1L
      metricsRegistry.getTimers.get("Timer-ConsumedAPI-AUTH-GetEnrolments-GET").getCount should be >= 1L
    }

    "return None if 6 digit agent reference cannot be found" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasNoSaAgentReference()

      await(newAuthConnector().currentAgentIdentifiers) shouldBe Some((None, "0000001232456789"))
    }

    "return None if the user is not logged in" in {
      given()
        .agentAdmin("ABCDEF123456").isNotLoggedIn()

      await(newAuthConnector().currentAgentIdentifiers) shouldBe None
    }

    "return a failed future if any errors happen" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andGettingEnrolmentsFailsWith500()

      an[Exception] shouldBe thrownBy {
        await(newAuthConnector().currentAgentIdentifiers)
      }
    }

    def newAuthConnector() = new AuthConnector(new URL(wiremockBaseUrl), WSHttp)
  }

}
