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
import uk.gov.hmrc.agentaccesscontrol.models.{
  AuthEnrolment,
  EnrolmentIdentifier,
  Enrolments
}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference

class EnrolmentsSpec extends UnitTest {

  "AuthEnrolment.identifier" should {
    "return None" when {
      "the list of identifiers is empty" in {
        AuthEnrolment("key", Seq.empty, "state")
          .identifier("empty") shouldBe None
      }
      "the provided identifier key does not match an identifier in the list" in {
        AuthEnrolment("key",
                      Seq(EnrolmentIdentifier("UTR", "1234567890")),
                      "state").identifier("TaxOfficeNo") shouldBe None
      }
    }
    "return Some(identifier)" when {
      "the identifier key matches an identifier in the list" in {
        AuthEnrolment("key",
                      Seq(EnrolmentIdentifier("UTR", "1234567890")),
                      "state").identifier("UTR") shouldBe Some("1234567890")
      }
    }
  }

  "Enrolments.saAgentReferenceOption" should {
    "return None" when {
      "no enrolments exist" in {
        Enrolments(Set.empty).saAgentReferenceOption shouldBe None
      }
      "no IR-SA-AGENT enrolment exists" in {
        Enrolments(Set(
          AuthEnrolment("HMRC-AS-AGENT",
                        Seq(EnrolmentIdentifier("AgentReferenceNumber", "arn")),
                        "State"))).saAgentReferenceOption shouldBe None
      }
    }
    "return Some(SaAgentReference)" when {
      "IR-SA-AGENT enrolment exists" in {
        Enrolments(
          Set(
            AuthEnrolment(
              "IR-SA-AGENT",
              Seq(EnrolmentIdentifier("IRAgentReference", "enrol-123")),
              "State"))).saAgentReferenceOption shouldBe Some(
          SaAgentReference("enrol-123"))
      }
    }
  }

  "Enrolments.arnOption" should {
    "return None" when {
      "no enrolments exist" in {
        Enrolments(Set.empty).arnOption shouldBe None
      }
      "no HMRC-AS-AGENT enrolment exists" in {
        Enrolments(
          Set(
            AuthEnrolment(
              "IR-SA-AGENT",
              Seq(EnrolmentIdentifier("IRAgentReference", "enrol-123")),
              "State"))).arnOption shouldBe None
      }
    }
    "return Some(AgentReferenceNumber)" when {
      "HMRC-AS-AGENT enrolment exists" in {
        Enrolments(Set(
          AuthEnrolment("HMRC-AS-AGENT",
                        Seq(EnrolmentIdentifier("AgentReferenceNumber", "arn")),
                        "State"))).arnOption shouldBe Some(Arn("arn"))
      }
    }
  }

}
