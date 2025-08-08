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

import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
import uk.gov.hmrc.domain.SaAgentReference

class EnrolmentsSpec extends UnitSpec {

  "AuthEnrolment.identifier" should {
    "return None" when {
      "the list of identifiers is empty" in {
        AuthEnrolment("key", Seq.empty, "state")
          .identifier("empty") mustBe None
      }
      "the provided identifier key does not match an identifier in the list" in {
        AuthEnrolment("key", Seq(EnrolmentIdentifier("UTR", "1234567890")), "state")
          .identifier("TaxOfficeNo") mustBe None
      }
    }
    "return Some(identifier)" when {
      "the identifier key matches an identifier in the list" in {
        AuthEnrolment("key", Seq(EnrolmentIdentifier("UTR", "1234567890")), "state").identifier("UTR") mustBe Some(
          "1234567890"
        )
      }
    }
  }

  "Enrolments.saAgentReferenceOption" should {
    "return None" when {
      "no enrolments exist" in {
        Enrolments(Set.empty).saAgentReferenceOption mustBe None
      }
      "no IR-SA-AGENT enrolment exists" in {
        Enrolments(
          Set(AuthEnrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "arn")), "State"))
        ).saAgentReferenceOption mustBe None
      }
    }
    "return Some(SaAgentReference)" when {
      "IR-SA-AGENT enrolment exists" in {
        Enrolments(
          Set(AuthEnrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "enrol-123")), "State"))
        ).saAgentReferenceOption mustBe Some(SaAgentReference("enrol-123"))
      }
    }
  }

  "Enrolments.arnOption" should {
    "return None" when {
      "no enrolments exist" in {
        Enrolments(Set.empty).arnOption mustBe None
      }
      "no HMRC-AS-AGENT enrolment exists" in {
        Enrolments(
          Set(AuthEnrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "enrol-123")), "State"))
        ).arnOption mustBe None
      }
    }
    "return Some(AgentReferenceNumber)" when {
      "HMRC-AS-AGENT enrolment exists" in {
        Enrolments(
          Set(AuthEnrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "arn")), "State"))
        ).arnOption mustBe Some(Arn("arn"))
      }
    }
  }

}
