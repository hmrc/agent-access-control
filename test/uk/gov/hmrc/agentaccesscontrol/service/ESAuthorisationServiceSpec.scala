/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.service

import org.scalamock.scalatest.MockFactory
import play.api.test.FakeRequest
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.config.AppConfig
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.model.{
  AgentRecord,
  AuthDetails,
  SuspensionDetails
}
import uk.gov.hmrc.agentaccesscontrol.support.AuditSupport
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ESAuthorisationServiceSpec
    extends UnitSpec
    with MockFactory
    with AuditSupport {

  val relationshipsConnector = mock[RelationshipsConnector]
  val auditService = mock[AuditService]
  val desAgentClientApiConnector = mock[DesAgentClientApiConnector]

  val servicesConfig = mock[ServicesConfig]
  (servicesConfig
    .baseUrl(_: String))
    .expects(*)
    .atLeastOnce()
    .returning("blah-url")
  (servicesConfig
    .getConfString(_: String, _: String))
    .expects(*, *)
    .atLeastOnce()
    .returning("blah-url")

  (servicesConfig
    .getBoolean(_: String))
    .expects("features.enable-agent-suspension")
    .returning(true)

  (servicesConfig
    .getBoolean(_: String))
    .expects("features.allowPayeAccess")
    .returning(true)

  implicit val appConfig = new AppConfig(servicesConfig)

  val service = new ESAuthorisationService(relationshipsConnector,
                                           desAgentClientApiConnector,
                                           auditService)

  val agentCode = AgentCode("agentCode")
  val arn = Arn("arn")
  val saAgentRef = SaAgentReference("ABC456")
  val clientId = MtdItId("clientId")
  val mtdAuthDetails =
    AuthDetails(None, Some(arn), "ggId", Some("Agent"), Some(User))
  val nonMtdAuthDetails =
    AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(User))
  implicit val hc = HeaderCarrier()
  implicit val fakeRequest =
    FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent/arn/client/utr")
  val agentRecord = AgentRecord(Some(SuspensionDetails(false, None)))

  "authoriseForMtdIt" should {
    "allow access for agent with a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning true
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      givenAuditEvent()
//      whenDesAgentClientApiConnectorIsCalled returning Future(
//        Right(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, nonMtdAuthDetails))

      result shouldBe false
    }

    "deny access for a mtd agent without a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning false
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning true
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))
      }

      "decision is made to deny access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning false
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))
      }

      "no HMRC-AS-AGENT enrolment exists" in {

        givenAuditEvent()

//        whenDesAgentClientApiConnectorIsCalled returning Future(
//          Right(agentRecord))

        await(service.authoriseForMtdIt(agentCode, clientId, nonMtdAuthDetails))
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("ITSA")))))

      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  "authoriseForMtdVat" should {

    val vrn = Vrn("vrn")

    "allow access for agent with a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning true
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      givenAuditEvent()
//      whenDesAgentClientApiConnectorIsCalled returning Future(
//        Right(agentRecord))

      val result =
        await(service.authoriseForMtdVat(agentCode, vrn, nonMtdAuthDetails))

      result shouldBe false
    }

    "deny access for a mtd agent without a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning false
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning true
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))
      }

      "decision is made to deny access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning false
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))
      }

      "no HMRC-AS-AGENT enrolment exists" in {

        givenAuditEvent()
//        whenDesAgentClientApiConnectorIsCalled returning Future(
//          Right(agentRecord))
        await(service.authoriseForMtdVat(agentCode, vrn, nonMtdAuthDetails))
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("ALL")))))

      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  "authoriseForTrust" should {

    val utr = Utr("utr")

    "allow access for agent with a client relationship" in {
      givenAuditEvent()
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))
      whenRelationshipsConnectorIsCalled returning true

      val result =
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      givenAuditEvent()
//      whenDesAgentClientApiConnectorIsCalled returning Future(
//        Right(agentRecord))
      val result =
        await(service.authoriseForTrust(agentCode, utr, nonMtdAuthDetails))

      result shouldBe false
    }

    "deny access for a mtd agent without a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning false
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning true
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))
      }

      "decision is made to deny access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning false
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

      }

      "no HMRC-AS-AGENT enrolment exists" in {
        givenAuditEvent()
//        whenDesAgentClientApiConnectorIsCalled returning Future(
//          Right(agentRecord))
        await(service.authoriseForTrust(agentCode, utr, nonMtdAuthDetails))
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("TRS")))))

      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

      result shouldBe false
    }
  }

  "authoriseForNonTaxableTrust" should {

    val urn = Urn("urn")

    "allow access for agent with a client relationship" in {
      givenAuditEvent()
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))
      whenRelationshipsConnectorIsCalled returning true

      val result =
        await(service.authoriseForTrust(agentCode, urn, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      givenAuditEvent()
      val result =
        await(service.authoriseForTrust(agentCode, urn, nonMtdAuthDetails))
      result shouldBe false
    }

    "deny access for a mtd agent without a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning false
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForTrust(agentCode, urn, mtdAuthDetails))

      result shouldBe false
    }

    "handle suspended agents and return false" in {

      val agentRecord = AgentRecord(
        Some(SuspensionDetails(suspensionStatus = true, Some(Set("TRS")))))

      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForTrust(agentCode, urn, mtdAuthDetails))
      result shouldBe false
    }
  }

  "authoriseForCgt" should {

    val cgtRef = CgtRef("XMCGTP123456789")

    "allow access for agent with a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning true
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      givenAuditEvent()
//      whenDesAgentClientApiConnectorIsCalled returning Future(
//        Right(agentRecord))

      val result =
        await(service.authoriseForCgt(agentCode, cgtRef, nonMtdAuthDetails))

      result shouldBe false
    }

    "deny access for a mtd agent without a client relationship" in {
      givenAuditEvent()
      whenRelationshipsConnectorIsCalled returning false
      whenDesAgentClientApiConnectorIsCalled returning Future(
        Right(agentRecord))

      val result =
        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning true
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))
      }

      "decision is made to deny access" in {
        givenAuditEvent()
        whenRelationshipsConnectorIsCalled returning false
        whenDesAgentClientApiConnectorIsCalled returning Future(
          Right(agentRecord))

        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))
      }

      "no HMRC-AS-AGENT enrolment exists" in {

        givenAuditEvent()

//        whenDesAgentClientApiConnectorIsCalled.returning(
//          Future(Right(agentRecord)))

        await(service.authoriseForCgt(agentCode, cgtRef, nonMtdAuthDetails))
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("CGT")))))

      whenDesAgentClientApiConnectorIsCalled.returning(
        Future(Right(agentRecord)))

      val result =
        await(service.authoriseForCgt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  def whenRelationshipsConnectorIsCalled =
    (relationshipsConnector
      .relationshipExists(_: Arn, _: MtdItId)(_: ExecutionContext,
                                              _: HeaderCarrier))
      .expects(*, *, *, *)

  def whenDesAgentClientApiConnectorIsCalled =
    (desAgentClientApiConnector
      .getAgentRecord(_: TaxIdentifier)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
}
