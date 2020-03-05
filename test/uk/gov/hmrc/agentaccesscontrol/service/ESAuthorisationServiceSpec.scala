/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Configuration
import play.api.test.FakeRequest
import uk.gov.hmrc.agentaccesscontrol.audit.AgentAccessControlEvent.AgentAccessControlDecision
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.connectors.AuthDetails
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentaccesscontrol.model.{AgentRecord, SuspensionDetails}
import uk.gov.hmrc.agentaccesscontrol.support.ResettingMockitoSugar
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, CgtRef, MtdItId, Utr, Vrn}
import uk.gov.hmrc.auth.core.Admin
import uk.gov.hmrc.domain.{AgentCode, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ESAuthorisationServiceSpec extends UnitSpec with ResettingMockitoSugar {

  val relationshipsConnector = resettingMock[RelationshipsConnector]
  val auditService = resettingMock[AuditService]
  val desAgentClientApiConnector = resettingMock[DesAgentClientApiConnector]
  implicit val config = resettingMock[Configuration]
  when(config.getBoolean(any[String])).thenReturn(Some(true))

  val service = new ESAuthorisationService(relationshipsConnector,
                                           desAgentClientApiConnector,
                                           auditService)

  val agentCode = AgentCode("agentCode")
  val arn = Arn("arn")
  val saAgentRef = SaAgentReference("ABC456")
  val clientId = MtdItId("clientId")
  val mtdAuthDetails =
    AuthDetails(None, Some(arn), "ggId", Some("Agent"), Some(Admin))
  val nonMtdAuthDetails =
    AuthDetails(Some(saAgentRef), None, "ggId", Some("Agent"), Some(Admin))
  implicit val hc = HeaderCarrier()
  implicit val fakeRequest =
    FakeRequest("GET", "/agent-access-control/mtd-it-auth/agent/arn/client/utr")
  val agentRecord = AgentRecord(Some(SuspensionDetails(false, None)))

  "authoriseForMtdIt" should {
    "allow access for agent with a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn true
      whenDesAgentClientApiConnectorIsCalled thenReturn (Future(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {

      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, nonMtdAuthDetails))

      result shouldBe false
      verify(relationshipsConnector, never)
        .relationshipExists(any[Arn], any[MtdItId])(any[ExecutionContext],
                                                    any[HeaderCarrier])
    }

    "deny access for a mtd agent without a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn false
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {

        whenRelationshipsConnectorIsCalled thenReturn true
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-MTD-IT",
                      clientId,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> true,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "decision is made to deny access" in {

        whenRelationshipsConnectorIsCalled thenReturn false
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-MTD-IT",
                      clientId,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> false,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "no HMRC-AS-AGENT enrolment exists" in {

        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForMtdIt(agentCode, clientId, nonMtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-MTD-IT",
                      clientId,
                      Seq("credId" -> "ggId", "accessGranted" -> false))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("ITSA")))))

      whenDesAgentClientApiConnectorIsCalled thenReturn (Future(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  "authoriseForMtdVat" should {

    val vrn = Vrn("vrn")

    "allow access for agent with a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn true
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {

      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForMtdVat(agentCode, vrn, nonMtdAuthDetails))

      result shouldBe false
      verify(relationshipsConnector, never)
        .relationshipExists(any[Arn], any[MtdItId])(any[ExecutionContext],
                                                    any[HeaderCarrier])
    }

    "deny access for a mtd agent without a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn false
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {

        whenRelationshipsConnectorIsCalled thenReturn true
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-MTD-VAT",
                      vrn,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> true,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "decision is made to deny access" in {

        whenRelationshipsConnectorIsCalled thenReturn false
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForMtdVat(agentCode, vrn, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-MTD-VAT",
                      vrn,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> false,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "no HMRC-AS-AGENT enrolment exists" in {
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)
        await(service.authoriseForMtdVat(agentCode, vrn, nonMtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-MTD-VAT",
                      vrn,
                      Seq("credId" -> "ggId", "accessGranted" -> false))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("ALL")))))

      whenDesAgentClientApiConnectorIsCalled thenReturn (Future(agentRecord))

      val result =
        await(service.authoriseForMtdIt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  "authoriseForTrust" should {

    val utr = Utr("utr")

    "allow access for agent with a client relationship" in {
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)
      whenRelationshipsConnectorIsCalled thenReturn true

      val result =
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)
      val result =
        await(service.authoriseForTrust(agentCode, utr, nonMtdAuthDetails))

      result shouldBe false
      verify(relationshipsConnector, never)
        .relationshipExists(any[Arn], any[MtdItId])(any[ExecutionContext],
                                                    any[HeaderCarrier])
    }

    "deny access for a mtd agent without a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn false
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {
        whenRelationshipsConnectorIsCalled thenReturn true
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-TERS-ORG",
                      utr,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> true,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "decision is made to deny access" in {

        whenRelationshipsConnectorIsCalled thenReturn false
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)
        await(service.authoriseForTrust(agentCode, utr, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-TERS-ORG",
                      utr,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> false,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "no HMRC-AS-AGENT enrolment exists" in {
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)
        await(service.authoriseForTrust(agentCode, utr, nonMtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-TERS-ORG",
                      utr,
                      Seq("credId" -> "ggId", "accessGranted" -> false))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("TRS")))))

      whenDesAgentClientApiConnectorIsCalled thenReturn (Future(agentRecord))

      val result =
        await(service.authoriseForTrust(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  "authoriseForCgt" should {

    val cgtRef = CgtRef("XMCGTP123456789")

    "allow access for agent with a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn true
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))

      result shouldBe true
    }

    "deny access for a non-mtd agent" in {

      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForCgt(agentCode, cgtRef, nonMtdAuthDetails))

      result shouldBe false
      verify(relationshipsConnector, never)
        .relationshipExists(any[Arn], any[MtdItId])(any[ExecutionContext],
                                                    any[HeaderCarrier])
    }

    "deny access for a mtd agent without a client relationship" in {

      whenRelationshipsConnectorIsCalled thenReturn false
      whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

      val result =
        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))

      result shouldBe false
    }

    "audit appropriate values" when {
      "decision is made to allow access" in {

        whenRelationshipsConnectorIsCalled thenReturn true
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-CGT-PD",
                      cgtRef,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> true,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "decision is made to deny access" in {

        whenRelationshipsConnectorIsCalled thenReturn false
        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForCgt(agentCode, cgtRef, mtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-CGT-PD",
                      cgtRef,
                      Seq("credId" -> "ggId",
                          "accessGranted" -> false,
                          "arn" -> arn.value))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }

      "no HMRC-AS-AGENT enrolment exists" in {

        whenDesAgentClientApiConnectorIsCalled thenReturn Future(agentRecord)

        await(service.authoriseForCgt(agentCode, cgtRef, nonMtdAuthDetails))

        verify(auditService)
          .auditEvent(AgentAccessControlDecision,
                      "agent access decision",
                      agentCode,
                      "HMRC-CGT-PD",
                      cgtRef,
                      Seq("credId" -> "ggId", "accessGranted" -> false))(
            hc,
            fakeRequest,
            concurrent.ExecutionContext.Implicits.global)
      }
    }

    "handle suspended agents and return false" in {

      val agentRecord =
        AgentRecord(Some(SuspensionDetails(true, Some(Set("CGT")))))

      whenDesAgentClientApiConnectorIsCalled thenReturn (Future(agentRecord))

      val result =
        await(service.authoriseForCgt(agentCode, clientId, mtdAuthDetails))

      result shouldBe false
    }
  }

  def whenRelationshipsConnectorIsCalled =
    when(
      relationshipsConnector.relationshipExists(any[Arn], any[MtdItId])(
        any[ExecutionContext],
        any[HeaderCarrier]))

  def whenDesAgentClientApiConnectorIsCalled =
    when(
      desAgentClientApiConnector.getAgentRecord(any[TaxIdentifier])(
        any[HeaderCarrier],
        any[ExecutionContext]))

}
