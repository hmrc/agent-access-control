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
import org.scalatest.TestData
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneServerPerTest}
import play.api.Application
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.StartAndStopWireMock
import uk.gov.hmrc.agentaccesscontrol.stubs.DataStreamStub
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.{Vrn => _, _}

import java.time.LocalDateTime

abstract class WireMockISpec extends UnitSpec with StartAndStopWireMock with StubUtils {

  protected val wireMockAppConfiguration: Map[String, Any] = Map(
    "microservice.services.des.host"                        -> wiremockHost,
    "microservice.services.des.port"                        -> wiremockPort,
    "microservice.services.des-paye.host"                   -> wiremockHost,
    "microservice.services.des-paye.port"                   -> wiremockPort,
    "microservice.services.des-sa.host"                     -> wiremockHost,
    "microservice.services.des-sa.port"                     -> wiremockPort,
    "microservice.services.auth.host"                       -> wiremockHost,
    "microservice.services.auth.port"                       -> wiremockPort,
    "microservice.services.enrolment-store-proxy.host"      -> wiremockHost,
    "microservice.services.enrolment-store-proxy.port"      -> wiremockPort,
    "auditing.enabled"                                      -> false,
    "auditing.consumer.baseUri.host"                        -> wiremockHost,
    "auditing.consumer.baseUri.port"                        -> wiremockPort,
    "microservice.services.agent-client-relationships.host" -> wiremockHost,
    "microservice.services.agent-client-relationships.port" -> wiremockPort,
    "microservice.services.agent-mapping.host"              -> wiremockHost,
    "microservice.services.agent-mapping.port"              -> wiremockPort,
    "microservice.services.agent-fi-relationship.host"      -> wiremockHost,
    "microservice.services.agent-fi-relationship.port"      -> wiremockPort,
    "microservice.services.agent-permissions.host"          -> wiremockHost,
    "microservice.services.agent-permissions.port"          -> wiremockPort,
    "features.allowPayeAccess" -> true
  )

  protected def additionalConfiguration: Map[String, String] = Map.empty

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(wireMockAppConfiguration ++ additionalConfiguration)
}

abstract class WireMockWithOneAppPerSuiteISpec extends WireMockISpec with GuiceOneAppPerSuite {
  override implicit lazy val app: Application = appBuilder.build()
}

abstract class WireMockWithOneServerPerTestISpec extends WireMockISpec with GuiceOneServerPerTest {
  override def newAppForTest(testData: TestData): Application = appBuilder.build()
}

trait StubUtils {
  me: StartAndStopWireMock =>

  class PreconditionBuilder {
    def agentAdmin(agentCode: AgentCode, providerId: String, saAgentReference: Option[SaAgentReference], arn: Option[Arn]): AgentAdmin =
      new AgentAdmin(agentCode.value, providerId, saAgentReference, arn)

    def mtdAgency(arn: Arn): MtdAgency =
      new MtdAgency(arn)
  }

  def given() =
    new PreconditionBuilder()

  class AgentAdmin(
                   override val agentCode: String,
                   override val providerId: String,
                   override val saAgentReference: Option[SaAgentReference],
                   override val arn: Option[Arn])
      extends AfiStub[AgentAdmin]
      with AuthStubs[AgentAdmin]
      with DesStub[AgentAdmin]
      with EnrolmentStoreProxyStubs[AgentAdmin]
      with MappingStubs[AgentAdmin] {
    DataStreamStub.givenAuditConnector()
  }

  class MtdAgency(override val arn: Arn) extends RelationshipsStub[MtdAgency] with MappingStubs[MtdAgency] {
    DataStreamStub.givenAuditConnector()
  }

  trait AfiStub[A] {
    me: A =>

    def andHasRelationship(arn: Arn, clientId: Nino): A = {
      stubFor(
        get(urlPathMatching(
          s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}"))
          .willReturn(aResponse().withStatus(200)))
      this
    }

    def andHasNoRelationship(arn: Arn, clientId: Nino): A = {
      stubFor(
        get(urlPathMatching(
          s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}"))
          .willReturn(aResponse().withStatus(404)))
      this
    }

    def statusReturnedForRelationship(arn: Arn, clientId: Nino, statusCode: Int): A = {
      stubFor(
        get(urlPathMatching(
          s"/agent-fi-relationship/relationships/PERSONAL-INCOME-RECORD/agent/${arn.value}/client/${clientId.value}"))
          .willReturn(aResponse().withStatus(statusCode)))
      this
    }
  }

  trait DesStub[A] {
    me: A with AuthStubs[A] =>

    def andDesIsDown(): A = {
      stubFor(get(urlPathMatching("/sa/agents/[^/]+/client/[^/]+")).willReturn(aResponse().withStatus(500)))
      stubFor(
        get(urlPathMatching("/agents/regime/PAYE/agent/[^/]+/client/[^/]+")).willReturn(aResponse().withStatus(500)))
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

    def andIsRelatedToSaClientInDes(
      clientUtr: SaUtr,
      authorizationHeader: String = "secret",
      envHeader: String = "test"): SaDesStubBuilder =
      new SaDesStubBuilder(clientUtr, authorizationHeader, envHeader)

    def andIsRelatedToPayeClientInDes(
      empRef: EmpRef,
      authorizationHeader: String = "secret",
      envHeader: String = "test"): PayeDesStubBuilder =
      new PayeDesStubBuilder(empRef, authorizationHeader, envHeader)

    private def matcherForClient(client: SaUtr) =
      get(urlPathEqualTo(s"/sa/agents/${saAgentReference.get.value}/client/${client.value}"))

    private def matcherForClient(empRef: EmpRef) =
      get(
        urlPathEqualTo(
          s"/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}"))

    class SaDesStubBuilder(client: SaUtr, authorizationToken: String, environment: String) {
      def andIsAuthorisedByOnly648(): A = withFlags(true, false)

      def andIsAuthorisedByOnlyI648(): A = withFlags(false, true)

      def butIsNotAuthorised(): A = withFlags(false, false)

      def andAuthorisedByBoth648AndI648(): A = withFlags(true, true)

      private def withFlags(auth_64_8: Boolean, auth_i64_8: Boolean): A = {
        stubFor(
          matcherForClient(client)
            .withHeader("Authorization", equalTo(s"Bearer $authorizationToken"))
            .withHeader("Environment", equalTo(environment))
            .willReturn(aResponse().withStatus(200).withBody(s"""
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
        stubFor(
          matcherForClient(client)
            .withHeader("Authorization", equalTo(s"Bearer $authorizationToken"))
            .withHeader("Environment", equalTo(environment))
            .willReturn(aResponse().withStatus(200).withBody(s"""
                                                                |{
                                                                |    "Auth_64-8": $auth_64_8
                                                                |}
        """.stripMargin)))
        DesStub.this
      }
    }

    def  givenAgentRecord(taxId: TaxIdentifier, suspended: Boolean, regime: String) = {
      stubFor(
        get(
          urlPathEqualTo(
            s"/registration/personal-details/arn/${taxId.value}")
        ).willReturn(aResponse().withStatus(200)
          .withBody(s"""{"suspensionDetails":{"suspensionStatus":$suspended,"regimes":["$regime"]}}"""))
      )
    }
  }

  trait MappingStubs[A] {
    me: A =>


    def givenSaMappingSingular(key: String, arn: Arn, identifier: String): A = {
      stubFor(
        get(urlMatching(s"/agent-mapping/mappings/key/$key/arn/${arn.value}"))
          .willReturn(aResponse().withBody(s"""
                                              |{
                                              |  "mappings":[
                                              |    {
                                              |      "arn":"${arn.value}",
                                              |      "identifier":"$identifier"
                                              |    }
                                              |  ]
                                              |}
             """.stripMargin)))
      this
    }

    def givenSaMappingMultiple(key: String, arn: Arn, identifier: String): A = {
      stubFor(
        get(urlMatching(s"/agent-mapping/mappings/key/$key/arn/${arn.value}"))
          .willReturn(aResponse().withBody(s"""
                                              |{
                                              |  "mappings":[
                                              |    {
                                              |      "arn":"${arn.value}",
                                              |      "identifier":"$identifier"
                                              |    },
                                              |    {
                                              |      "arn":"${arn.value}",
                                              |      "identifier":"SA6012"
                                              |    },
                                              |    {
                                              |      "arn":"${arn.value}",
                                              |      "identifier":"A1709A"
                                              |    }
                                              |  ]
                                              |}
             """.stripMargin)))
      this
    }

    def givenNotFound404Mapping(key: String, arn: Arn): A = {
      stubFor(
        get(urlMatching(s"/agent-mapping/mappings/key/$key/arn/${arn.value}"))
          .willReturn(aResponse().withStatus(404)))
      this
    }

    def givenBadRequest400Mapping(key: String, arn: Arn): A = {
      stubFor(
        get(urlMatching(s"/agent-mapping/mappings/key/$key/arn/${arn.value}"))
          .willReturn(aResponse().withStatus(400)))
      this
    }

    def givenServiceUnavailable502Mapping(key: String, arn: Arn): A = {
      stubFor(
        get(urlMatching(s"/agent-mapping/mappings/key/$key/arn/${arn.value}"))
          .willReturn(aResponse().withStatus(502)))
      this
    }
  }

  trait EnrolmentStoreProxyStubs[A] {
    me: A =>

    def providerId: String

    private val pathRegex: String = "/enrolment-store-proxy/enrolment-store/enrolments/[^/]+/users\\?type=delegated"

    private def pathDelegated(enrolmentKey: String): String =
      s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=delegated"
    private def pathPrincipal(enrolmentKey: String): String =
      s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=principal"

    private def getES0Delegated(id: TaxIdentifier) = {
      val enrolmentKey = id match {
        case utr: SaUtr => s"IR-SA~UTR~$utr"
        case empRef: EmpRef =>
          s"IR-PAYE~TaxOfficeNumber~${empRef.taxOfficeNumber}~TaxOfficeReference~${empRef.taxOfficeReference}"
      }

      get(urlEqualTo(pathDelegated(enrolmentKey)))
    }

    private def getES0Principal(id: TaxIdentifier) = {
      val enrolmentKey = id match {
        case saAgentRef: SaAgentReference => s"IR-SA-AGENT~IRAgentReference~${saAgentRef.value}"
      }
      get(urlEqualTo(pathPrincipal(enrolmentKey)))
    }

    def andEnrolmentStoreProxyReturnsAnError500(): A = {
      stubFor(
        get(urlMatching(pathRegex))
          .willReturn(aResponse().withStatus(500)))
      this
    }

    def andEnrolmentStoreProxyIsDown(id: TaxIdentifier): A = andEnrolmentStoreProxyReturnsAnError500()

    def andEnrolmentStoreProxyReturnsUnparseableJson(id: TaxIdentifier): A = {
      stubFor(
        getES0Delegated(id)
          .willReturn(aResponse()
            .withBody("Not Json!")))
      this
    }

    def andIsAssignedToClient(id: TaxIdentifier, otherDelegatedUserIds: String*): A = {
      stubFor(
        getES0Delegated(id)
          .willReturn(aResponse()
            .withBody(s"""
                         |{
                         |    "principalUserIds": [],
                         |    "delegatedUserIds": [
                         |       ${(otherDelegatedUserIds :+ providerId).map(a => "\"" + a + "\"").mkString(",")}
                         |    ]
                         |}
                         |""".stripMargin)))
      this
    }

    def andHasSaEnrolmentForAgent(id: TaxIdentifier, otherPrincipalUserIds: String*): A = {
      stubFor(
        getES0Principal(id)
          .willReturn(aResponse()
            .withBody(s"""
                         |{
                         |    "principalUserIds": [
                         |       ${(otherPrincipalUserIds :+ providerId).map(a => "\"" + a + "\"").mkString(",")}
                         |     ]
                         |}
                         |""".stripMargin)))
      this
    }

    def andIsNotAssignedToClient(id: TaxIdentifier): A = {
      stubFor(
        getES0Delegated(id)
          .willReturn(aResponse()
            .withBody(s"""
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
      stubFor(
        get(urlMatching(pathRegex))
          .willReturn(aResponse()
            .withBody(s"""
                         |{
                         |    "principalUserIds": [],
                         |    "delegatedUserIds": []
                         |}
                         |""".stripMargin)))
      this
    }

    def andEnrolmentStoreProxyReturns204NoContent: A = {
      stubFor(
        get(urlMatching(pathRegex))
          .willReturn(aResponse().withStatus(204)))
      this
    }
  }

  trait AuthStubs[A] {
    me: A =>

    def agentCode: String

    def providerId: String

    def saAgentReference: Option[SaAgentReference]

    def arn: Option[Arn]

    def authIsDown(): A = {
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .willReturn(aResponse()
            .withStatus(500)))
    this
    }

    def userIsNotAuthenticated(): A  = {
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("WWW-Authenticate", "MDTP detail=\"SessionRecordNotFound\"")))
      this
    }

    def userHasInsufficientEnrolments(): A = {
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))
      this
    }

    def userLoggedInViaUnsupportedAuthProvider(): A = {
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAuthProvider\"")))
      this
    }

    def userIsNotAnAgent(): A  = {
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAffinityGroup\"")))
      this
    }


    def isAuthenticated(): A = {
      val (enrolKey, identifierKey, identifierValue: String): (String, String, String) =
        if(arn.isDefined) ("HMRC-AS-AGENT", "AgentReferenceNumber", arn.get.value) else ("IR-SA-AGENT","IRAgentReference", saAgentReference.fold("")(_.value))

      givenAuthorisedFor(
        s"""
           |{
           |  "authorise": [
           |    { "authProviders": ["GovernmentGateway"] },
           |    { "affinityGroup" : "Agent"}
           |  ],
           |  "retrieve":["agentCode", "allEnrolments", "credentialRole", "optionalCredentials"]
           |}
           """.stripMargin,
        s"""
           |{
           |"agentCode" : "$agentCode",
           |"allEnrolments": [
           |  { "key": "$enrolKey", "identifiers": [
           |    {"key": "$identifierKey", "value": "$identifierValue"}
           |  ]}
           |],
           |"credentialRole": "user",
           |"optionalCredentials": {"providerId": "$providerId", "providerType": "GovernmentGateway"}
           |}
          """.stripMargin
      )
      this
    }

    def givenAuthorisedFor(payload: String, responseBody: String): A = {
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .withRequestBody(equalToJson(payload, true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody(responseBody)))
      this
    }
  }

  trait RelationshipsStub[A] {
    me: A =>
    def arn: Arn

    def hasARelationshipWith(identifier: TaxIdentifier): A = statusReturnedForRelationship(identifier, 200)

    def hasNoRelationshipWith(identifier: TaxIdentifier): A = statusReturnedForRelationship(identifier, 404)

    def hasAssignedRelationshipToAgentUser(identifier: TaxIdentifier, agentUserId: String): A = statusReturnedForUserLevelRelationship(identifier, agentUserId, 200)
    def hasNoAssignedRelationshipToAgentUser(identifier: TaxIdentifier, agentUserId: String): A = statusReturnedForUserLevelRelationship(identifier, agentUserId, 404)

    def isOptedInToGranularPermissions: A = statusReturnedForGPOptInRecordExists(NO_CONTENT)
    def isOptedOutOfGranularPermissions: A = statusReturnedForGPOptInRecordExists(NOT_FOUND)

    def userIsInTaxServiceGroup(taxService: String, agentUserId: String, excludedClients: Set[Client] = Set.empty): A = {
      val url = s"/agent-permissions/arn/${arn.value}/tax-group/$taxService"
      val response: TaxServiceAccessGroup =
        TaxServiceAccessGroup(
          arn = arn,
          groupName = taxService + "-group",
          created = LocalDateTime.now,
          lastUpdated = LocalDateTime.now,
          createdBy = AgentUser("someId", "someName"),
          lastUpdatedBy = AgentUser("someId", "someName"),
          teamMembers = Some(Set(AgentUser(agentUserId, "Johnny Agent"))),
          service = taxService,
          automaticUpdates = false,
          excludedClients = Some(excludedClients)
        )

      stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse()
            .withStatus(200)
          .withBody(Json.toJson(response).toString())))
      this
    }

    private def acrUrl(identifier: TaxIdentifier): String = identifier match {
      case _ @MtdItId(mtdItId) =>
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/$mtdItId"
      case _ @Vrn(vrn) => s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/$vrn"
      case _ @Utr(utr) => s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERS-ORG/client/SAUTR/$utr"
      case _ @CgtRef(cgtRef) => s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/$cgtRef"
      case _ @Urn(urn) => s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERSNT-ORG/client/URN/$urn"
      case _ @PptRef(pptRef) => s"/agent-client-relationships/agent/${arn.value}/service/HMRC-PPT-ORG/client/EtmpRegistrationNumber/$pptRef"
    }

    def statusReturnedForRelationship(identifier: TaxIdentifier, status: Int): A = {
      val url = acrUrl(identifier)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse()
            .withStatus(status)))
      this
    }

    def statusReturnedForUserLevelRelationship(identifier: TaxIdentifier, agentUserId: String, status: Int): A = {
      val url = acrUrl(identifier) + s"?userId=$agentUserId"

      stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse()
            .withStatus(status)))
      this
    }

    def statusReturnedForGPOptInRecordExists(status: Int): A = {
      val url = s"/agent-permissions/arn/${arn.value}/optin-record-exists"

      stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse()
            .withStatus(status)))
      this
    }
  }

}
