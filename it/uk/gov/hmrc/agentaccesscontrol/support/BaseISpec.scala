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

package uk.gov.hmrc.agentaccesscontrol.support

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.Mockito.verify
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, OneServerPerSuite}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentaccesscontrol.{StartAndStopWireMock, WSHttp}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.MergedDataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

abstract class WireMockISpec extends UnitSpec
  with StartAndStopWireMock
  with StubUtils {

  protected val wireMockAppConfiguration: Map[String, Any] = Map(
    "microservice.services.des.host" -> wiremockHost,
    "microservice.services.des.port" -> wiremockPort,
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort,
    "microservice.services.enrolment-store-proxy.host" -> wiremockHost,
    "microservice.services.enrolment-store-proxy.port" -> wiremockPort,
    "auditing.consumer.baseUri.host" -> wiremockHost,
    "auditing.consumer.baseUri.port" -> wiremockPort,
    "microservice.services.agent-client-relationships.host" -> wiremockHost,
    "microservice.services.agent-client-relationships.port" -> wiremockPort,
    "microservice.services.agent-fi-relationship.host" -> wiremockHost,
    "microservice.services.agent-fi-relationship.port" -> wiremockPort
  )
}

abstract class WireMockWithOneAppPerSuiteISpec extends WireMockISpec
  with OneAppPerSuite {

  protected def additionalConfiguration: Map[String, String] = Map.empty

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        wireMockAppConfiguration ++ additionalConfiguration
      )
  }
}

abstract class WireMockWithOneServerPerSuiteISpec extends WireMockISpec
  with OneServerPerSuite {

  protected def additionalConfiguration: Map[String, String] = Map.empty

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        wireMockAppConfiguration ++ additionalConfiguration
      )
  }
}

trait StubUtils {
  me: StartAndStopWireMock =>

  class PreconditionBuilder {
    def agentAdmin(agentCode: String, agentCredId: String = "0000001232456789"): AgentAdmin = {
      new AgentAdmin(agentCode, agentCredId, oid = "556737e15500005500eaf68e")
    }

    def agentAdmin(agentCode: AgentCode): AgentAdmin = {
      agentAdmin(agentCode.value)
    }

    def mtdAgency(arn: Arn): MtdAgency = {
      new MtdAgency(arn)
    }
  }

  def given() = {
    new PreconditionBuilder()
  }

  class AgentAdmin(override val agentCode: String,
                   override val agentCredId: String,
                   override val oid: String)
    extends AfiStub[AgentAdmin] with AuthStubs[AgentAdmin] with DesStub[AgentAdmin] with EnrolmentStoreProxyStubs[AgentAdmin]

  class MtdAgency(override val arn: Arn)
    extends RelationshipsStub[MtdAgency]

  trait AfiStub[A] {
    me: A =>

    def andHasRelationship(arn: Arn, clientId: Nino): A = {
      stubFor(get(urlPathMatching(s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}")).
        willReturn(aResponse().withStatus(200)))
      this
    }

    def andHasNoRelationship(arn: Arn, clientId: Nino): A = {
      stubFor(get(urlPathMatching(s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}")).
        willReturn(aResponse().withStatus(404)))
      this
    }

    def statusReturnedForRelationship(arn: Arn, clientId: Nino, statusCode: Int): A = {
      stubFor(get(urlPathMatching(s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}")).
        willReturn(aResponse().withStatus(statusCode)))
      this
    }
  }

  trait DesStub[A] {
    me: A with AuthStubs[A] =>

    def andDesIsDown(): A = {
      stubFor(get(urlPathMatching("/sa/agents/[^/]+/client/[^/]+")).
        willReturn(aResponse().withStatus(500)))
      stubFor(get(urlPathMatching("/agents/regime/PAYE/agent/[^/]+/client/[^/]+")).
        willReturn(aResponse().withStatus(500)))
      this
    }

    def andHasNoRelationInDesWith(client: SaUtr): A = {
      stubFor(matcherForClient(client).willReturn(aResponse().withStatus(404)))
      this
    }

    def andHasNoRelationInDesWith(client: EmpRef): A = {
      stubFor(matcherForClient(client).willReturn(aResponse().withStatus(404)))
      this
    }

    def andIsRelatedToSaClientInDes(clientUtr: SaUtr, authorizationHeader: String = "secret", envHeader: String = "test"): SaDesStubBuilder = {
      new SaDesStubBuilder(clientUtr, authorizationHeader, envHeader)
    }

    def andIsRelatedToPayeClientInDes(empRef: EmpRef, authorizationHeader: String = "secret", envHeader: String = "test"): PayeDesStubBuilder = {
      new PayeDesStubBuilder(empRef, authorizationHeader, envHeader)
    }

    private def matcherForClient(client: SaUtr) =
      get(urlPathEqualTo(s"/sa/agents/${saAgentReference.get.value}/client/${client.value}"))

    private def matcherForClient(empRef: EmpRef) =
      get(urlPathEqualTo(s"/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}"))

    class SaDesStubBuilder(client: SaUtr, authorizationToken: String, environment: String) {
      def andIsAuthorisedByOnly648(): A = withFlags(true, false)

      def andIsAuthorisedByOnlyI648(): A = withFlags(false, true)

      def butIsNotAuthorised(): A = withFlags(false, false)

      def andAuthorisedByBoth648AndI648(): A = withFlags(true, true)

      private def withFlags(auth_64_8: Boolean, auth_i64_8: Boolean): A = {
        stubFor(matcherForClient(client)
          .withHeader("Authorization", equalTo(s"Bearer $authorizationToken"))
          .withHeader("Environment", equalTo(environment))
          .willReturn(aResponse().withStatus(200).withBody(
            s"""
               |{
               |    "Auth_64-8": $auth_64_8,
               |    "Auth_i64-8": $auth_i64_8
               |}
        """.stripMargin)))
        DesStub.this
      }
    }

    class PayeDesStubBuilder(client: EmpRef, authorizationToken: String, environment: String) {
      def andIsAuthorisedBy648(): A = withFlags(true)

      def butIsNotAuthorised(): A = withFlags(false)

      private def withFlags(auth_64_8: Boolean): A = {
        stubFor(matcherForClient(client)
          .withHeader("Authorization", equalTo(s"Bearer $authorizationToken"))
          .withHeader("Environment", equalTo(environment))
          .willReturn(aResponse().withStatus(200).withBody(
            s"""
               |{
               |    "Auth_64-8": $auth_64_8
               |}
        """.stripMargin)))
        DesStub.this
      }
    }

  }

  trait EnrolmentStoreProxyStubs[A] {
    me: A =>

    def agentCredId: String

    private val pathRegex: String = "/enrolment-store-proxy/enrolment-store/enrolments/[^/]+/users\\?type=delegated"

    private def path(enrolmentKey: String): String = s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=delegated"

    private def getES0(id: TaxIdentifier) = {
      val enrolmentKey = id match {
        case utr: SaUtr => s"IR-SA~UTR~$utr"
        case empRef: EmpRef => s"IR-PAYE~TaxOfficeNumber~${empRef.taxOfficeNumber}~TaxOfficeReference~${empRef.taxOfficeReference}"
      }

      get(urlEqualTo(path(enrolmentKey)))
    }

    def andEnrolmentStoreProxyReturnsAnError500(): A = {
      stubFor(get(urlMatching(pathRegex))
        .willReturn(aResponse().withStatus(500)))
      this
    }

    def andEnrolmentStoreProxyIsDown(id: TaxIdentifier): A = andEnrolmentStoreProxyReturnsAnError500()

    def andEnrolmentStoreProxyReturnsUnparseableJson(id: TaxIdentifier): A = {
      stubFor(getES0(id)
        .willReturn(aResponse()
          .withBody("Not Json!")))
      this
    }

    def andIsAssignedToClient(id: TaxIdentifier): A = {
      stubFor(getES0(id)
        .willReturn(aResponse()
          .withBody(
            s"""
               |{
               |    "principalUserIds": [],
               |    "delegatedUserIds": [
               |       "$agentCredId",
               |       "98741987654321",
               |       "98741987654322"
               |    ]
               |}
               |""".stripMargin)))
      this
    }

    def andIsNotAssignedToClient(id: TaxIdentifier): A = {
      stubFor(getES0(id)
        .willReturn(aResponse()
          .withBody(
            s"""
               |{
               |    "principalUserIds": [],
               |    "delegatedUserIds": [
               |       "98741987654323",
               |       "98741987654324",
               |       "98741987654325"
               |    ]
               |}
               |""".stripMargin)))
      this
    }

    def andHasNoAssignmentsForAnyClient: A = {
      stubFor(get(urlMatching(pathRegex))
        .willReturn(aResponse()
          .withBody(
            s"""
               |{
               |    "principalUserIds": [],
               |    "delegatedUserIds": []
               |}
               |""".stripMargin)))
      this
    }

    def andEnrolmentStoreProxyReturns204NoContent: A = {
      stubFor(get(urlMatching(pathRegex))
        .willReturn(aResponse().withStatus(204)))
      this
    }
  }

  trait AuthStubs[A] {
    me: A =>

    def oid: String

    def agentCode: String

    def agentCredId: String

    protected var saAgentReference: Option[SaAgentReference] = None

    def andGettingEnrolmentsFailsWith500(): A = {
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(500)))
      this
    }

    def andIsNotEnrolledForSA() = andHasNoIrSaAgentEnrolment()

    def andHasNoSaAgentReference(): A = {
      saAgentReference = None
      andHasNoIrSaAgentEnrolment()
    }

    def andHasNoIrSaAgentEnrolment(): A = andHasNoIrSaAgentOrHmrcAsAgentEnrolment()

    def andHasNoHmrcAsAgentEnrolment(): A = andHasNoIrSaAgentOrHmrcAsAgentEnrolment()

    private def andHasNoIrSaAgentOrHmrcAsAgentEnrolment(): A = {
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"},
           | {"key":"HMRC-AGENT-AGENT","identifiers":[{"key":"AgentRefNumber","value":"JARN1234567"}],"state":"Activated"}]
         """.stripMargin
      )))
      this
    }

    def andHasSaAgentReference(saAgentReference: SaAgentReference): A = {
      andHasSaAgentReference(saAgentReference.value)
    }

    def andHasSaAgentReference(ref: String): A = {
      saAgentReference = Some(SaAgentReference(ref))
      this
    }

    def andHasSaAgentReferenceWithEnrolment(saAgentReference: SaAgentReference, enrolmentState: String = "Activated"): A = {
      andHasSaAgentReference(saAgentReference)
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"},
           | {"key":"HMRC-AGENT-AGENT","identifiers":[{"key":"AgentRefNumber","value":"JARN1234567"}],"state":"Activated"},
           | {"key":"IR-SA-AGENT","identifiers":[{"key":"AnotherIdentifier", "value": "not the IR Agent Reference"}, {"key":"IRAgentReference","value":"${saAgentReference.value}"}],"state":"$enrolmentState"}]
         """.stripMargin
      )))
      this
    }

    def andHasSaAgentReferenceAndArnWithEnrolments(saAgentReference: SaAgentReference, arn: uk.gov.hmrc.agentmtdidentifiers.model.Arn): A = {
      andHasSaAgentReference(saAgentReference)
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"},
           | {"key":"HMRC-AGENT-AGENT","identifiers":[{"key":"AgentRefNumber","value":"JARN1234567"}],"state":"Activated"},
           | {"key":"IR-SA-AGENT","identifiers":[{"key":"AnotherIdentifier", "value": "not the IR Agent Reference"}, {"key":"IRAgentReference","value":"${saAgentReference.value}"}],"state":"Activated"},
           | {"key":"HMRC-AS-AGENT","identifiers":[{"key":"AnotherIdentifier", "value": "not the ARN"}, {"key":"AgentReferenceNumber","value":"${arn.value}"}],"state":"Activated"}]
         """.stripMargin
      )))
      this
    }

    def andHasHmrcAsAgentEnrolment(arn: uk.gov.hmrc.agentmtdidentifiers.model.Arn): A = {
      stubFor(get(urlPathEqualTo(s"/auth/oid/$oid/enrolments")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |[{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IrAgentReference","value":"HZ1234"}],"state":"Activated"},
           | {"key":"HMRC-AGENT-AGENT","identifiers":[{"key":"AgentRefNumber","value":"JARN1234567"}],"state":"Activated"},
           | {"key":"HMRC-AS-AGENT","identifiers":[{"key":"AnotherIdentifier", "value": "not the ARN"}, {"key":"AgentReferenceNumber","value":"${arn.value}"}],"state":"Activated"}]
         """.stripMargin
      )))
      this
    }

    def andHasSaAgentReferenceWithPendingEnrolment(saAgentReference: SaAgentReference): A =
      andHasSaAgentReferenceWithEnrolment(saAgentReference, enrolmentState = "Pending")

    def isNotLoggedIn(): A = {
      stubFor(get(urlPathEqualTo(s"/auth/authority")).willReturn(aResponse().withStatus(401)))
      this
    }

    private val defaultOnlyUsedByAuditingAuthorityJson =
      s"""
         |  ,
         |  "accounts": {
         |    "agent": {
         |      "agentUserRole": "admin"
         |    }
         |  },
         |  "affinityGroup": "Agent"
       """.stripMargin

    def isLoggedIn(onlyUsedByAuditingAuthorityJson: String = defaultOnlyUsedByAuditingAuthorityJson): A = {
      stubFor(get(urlPathEqualTo(s"/auth/authority")).willReturn(aResponse().withStatus(200).withBody(
        s"""
           |{
           |  "enrolments":"/auth/oid/$oid/enrolments",
           |  "credentials":{
           |    "gatewayId":"$agentCredId"
           |  }
           |  $onlyUsedByAuditingAuthorityJson
           |}
       """.stripMargin
      )))
      this
    }
  }

  trait RelationshipsStub[A] {
    me: A =>
    def arn: Arn

    def hasARelationshipWith(clientId: MtdItId): A = statusReturnedForRelationship(clientId, 200)

    def hasNoRelationshipWith(clientId: MtdItId): A = statusReturnedForRelationship(clientId, 404)

    def statusReturnedForRelationship(clientId: MtdItId, status: Int): A = {
      stubFor(get(urlEqualTo(s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${clientId.value}"))
        .willReturn(aResponse()
          .withStatus(status)))
      this
    }
  }

  trait MockAuditingContext extends MockitoSugar with Eventually {
    val mockAuditConnector = mock[AuditConnector]
    val wsHttp = new WSHttp {
      override lazy val auditConnector = mockAuditConnector
    }

    def capturedEvent() = {
      // HttpAuditing.AuditingHook does the auditing asynchronously, so we need
      // to use eventually to avoid a race condition in this test
      eventually {
        val captor = ArgumentCaptor.forClass(classOf[MergedDataEvent])
        verify(mockAuditConnector).sendMergedEvent(captor.capture())(Matchers.any[HeaderCarrier], Matchers.any[ExecutionContext])
        captor.getValue
      }
    }
  }

}
