package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference

trait AuthStub extends WiremockMethods {

  val authUrl = "/auth/authorise"

  def successfulAuthResponse(
      agentCode: String,
      providerId: String,
      optArn: Option[Arn],
      optSaAgentReference: Option[SaAgentReference]
  ): JsObject = {

    val (enrolKey, identifierKey, identifierValue: String): (String, String, String) =
      optArn match {
        case Some(arn) => ("HMRC-AS-AGENT", "AgentReferenceNumber", arn.value)
        case _         => ("IR-SA-AGENT", "IRAgentReference", optSaAgentReference.map(_.value).getOrElse(""))
      }

    Json.obj(
      "agentCode" -> agentCode,
      "allEnrolments" -> Json.arr(
        Json.obj(
          "key" -> enrolKey,
          "identifiers" -> Json.arr(
            Json.obj(
              "key"   -> identifierKey,
              "value" -> identifierValue
            )
          )
        )
      ),
      "credentialRole" -> "user",
      "optionalCredentials" -> Json.obj(
        "providerId"   -> providerId,
        "providerType" -> "GovernmentGateway"
      )
    )
  }

  def stubAuth[T](status: Int, body: T)(implicit writes: Writes[T]): StubMapping =
    when(method = POST, uri = authUrl)
      .thenReturn(status = status, body = writes.writes(body))

  def stubAuthUserIsNotAgent(): StubMapping =
    when(method = POST, uri = authUrl)
      .thenReturn(
        status = UNAUTHORIZED,
        headers = Map(HeaderNames.WWW_AUTHENTICATE -> "MDTP detail=\"UnsupportedAffinityGroup\"")
      )

  def stubAuthUserIsNotAuthenticated(): StubMapping =
    when(method = POST, uri = authUrl)
      .thenReturn(
        status = UNAUTHORIZED,
        headers = Map(HeaderNames.WWW_AUTHENTICATE -> "MDTP detail=\"SessionRecordNotFound\"")
      )

  def stubUnsupportedAuthProvider(): StubMapping =
    when(method = POST, uri = authUrl)
      .thenReturn(
        status = UNAUTHORIZED,
        headers = Map(HeaderNames.WWW_AUTHENTICATE -> "MDTP detail=\"UnsupportedAuthProvider\"")
      )

  def stubAuthUserHasInsufficientEnrolments(): StubMapping =
    when(method = POST, uri = authUrl)
      .thenReturn(
        status = UNAUTHORIZED,
        headers = Map(HeaderNames.WWW_AUTHENTICATE -> "MDTP detail=\"InsufficientEnrolments\"")
      )

  def stubAuthInternalServerError(): StubMapping =
    when(method = POST, uri = authUrl).thenReturn(status = INTERNAL_SERVER_ERROR)

}
