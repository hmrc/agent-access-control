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

import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agentaccesscontrol.WSHttp
import uk.gov.hmrc.agentaccesscontrol.support.WireMockWithOneAppPerSuiteISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AuthConnectorISpec extends WireMockWithOneAppPerSuiteISpec {

  implicit val hc = HeaderCarrier()

  "currentAuthDetails" should {
    "return sa agent reference and other auth details" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasSaAgentReferenceAndArnWithEnrolments(SaAgentReference("REF879"), Arn("AARN0000002"))

      val authDetails: AuthDetails = await(newAuthConnector().currentAuthDetails).get
      authDetails.saAgentReference shouldBe Some(SaAgentReference("REF879"))
      authDetails.arn shouldBe Some(Arn("AARN0000002"))
      authDetails.ggCredentialId shouldBe "0000001232456789"
      authDetails.affinityGroup shouldBe Some("Agent")
      authDetails.agentUserRole shouldBe Some("admin")
    }

    // The auth team are planning to remove accounts, which is where we get
    // agentUserRole from, and we only extract these fields so that we can
    // include them in audit logs
    "handle affinityGroup and agentUserRole being missing" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn(onlyUsedByAuditingAuthorityJson = "")
        .andHasSaAgentReferenceWithEnrolment(SaAgentReference("REF879"))

      val authDetails: AuthDetails = await(newAuthConnector().currentAuthDetails).get
      authDetails.saAgentReference shouldBe Some(SaAgentReference("REF879"))
      authDetails.ggCredentialId shouldBe "0000001232456789"
      authDetails.affinityGroup shouldBe None
      authDetails.agentUserRole shouldBe None
    }

    "record metrics for both /auth/authority and /auth/authority/(oid)/enrolments" in {
      val metricsRegistry = app.injector.instanceOf[Metrics].defaultRegistry
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasSaAgentReferenceWithEnrolment(SaAgentReference("REF879"))

      val authDetails: AuthDetails = await(newAuthConnector().currentAuthDetails).get
      authDetails.saAgentReference shouldBe Some(SaAgentReference("REF879"))
      authDetails.ggCredentialId shouldBe "0000001232456789"

      metricsRegistry.getTimers.get("Timer-ConsumedAPI-AUTH-GetAuthority-GET").getCount should be >= 1L
      metricsRegistry.getTimers.get("Timer-ConsumedAPI-AUTH-GetEnrolments-GET").getCount should be >= 1L
    }

    "return None as the saAgentReference if 6 digit agent reference cannot be found" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasNoSaAgentReference()

      val authDetails: AuthDetails = await(newAuthConnector().currentAuthDetails).get
      authDetails.saAgentReference shouldBe None
      authDetails.ggCredentialId shouldBe "0000001232456789"
    }

    "return None as the ARN if no ARN found in the enrolments" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andHasNoIrSaAgentEnrolment()

      val authDetails: AuthDetails = await(newAuthConnector().currentAuthDetails).get
      authDetails.arn shouldBe None
      authDetails.ggCredentialId shouldBe "0000001232456789"
    }

    "return None if the user is not logged in" in {
      given()
        .agentAdmin("ABCDEF123456").isNotLoggedIn()

      await(newAuthConnector().currentAuthDetails) shouldBe None
    }

    "return a failed future if any errors happen" in {
      given()
        .agentAdmin("ABCDEF123456").isLoggedIn()
        .andGettingEnrolmentsFailsWith500()

      an[Exception] shouldBe thrownBy {
        await(newAuthConnector().currentAuthDetails)
      }
    }

    def newAuthConnector() = new AuthConnector(new URL(wiremockBaseUrl), WSHttp, app.injector.instanceOf[Metrics])
  }

}
