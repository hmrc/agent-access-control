package uk.gov.hmrc.agentaccesscontrol.stubs


import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object AuthStubs {

  def authIsDown(): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse()
          .withStatus(500)))

  def userIsNotAuthenticated(): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"SessionRecordNotFound\"")))

  def userHasInsufficientEnrolments(): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))

  def userLoggedInViaUnsupportedAuthProvider(): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAuthProvider\"")))

  def userIsNotAnAgent()  = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAffinityGroup\"")))
  }


  def authenticatedAgentFor(mtdAgent: Boolean): StubMapping = {
    val (enrolKey, identifierKey): (String, String) =
      if(mtdAgent) ("HMRC-AS-AGENT", "AgentReferenceNumber") else ("IR-SA-AGENT","IRAgentReference")

    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] },
         |    { "affinityGroup" : "Agent"}
         |  ],
         |  "retrieve":["agentCode","allEnrolments", "credentialRole", "optionalCredentials"]
         |}
           """.stripMargin,
      s"""
         |{
         |"agentCode" : "agent-123",
         |"allEnrolments": [
         |  { "key":$enrolKey, "identifiers": [
         |    {"key":$identifierKey, "value": "enrol-123"}
         |  ]}
         |],
         |"credentialRole": "user",
         |"optionalCredentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"}
         |}
          """.stripMargin
    )
  }

  def givenAuthorisedFor(payload: String, responseBody: String): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)))








}
