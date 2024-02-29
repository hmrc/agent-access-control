package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.SaUtr

trait EnrolmentStoreProxyStub extends WiremockMethods {

  def successfulResponseDelegated(userIds: Seq[String]): JsObject =
    Json.obj("principalUserIds" -> Json.arr(), "delegatedUserIds" -> userIds)

  def successfulResponsePrincipal(userIds: Seq[String]): JsObject =
    Json.obj("principalUserIds" -> userIds)

  def stubQueryUsersAssignedEnrolmentsDelegatedSa(saUtr: SaUtr)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/enrolment-store-proxy/enrolment-store/enrolments/IR-SA~UTR~${saUtr.value}/users\\?type=delegated"
    ).thenReturn(status, body)

  def stubQueryUsersAssignedEnrolmentsPrincipalSa(
      saAgentReference: SaAgentReference
  )(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri =
        s"/enrolment-store-proxy/enrolment-store/enrolments/IR-SA-AGENT~IRAgentReference~${saAgentReference.value}/users\\?type=principal"
    ).thenReturn(status, body)

  def stubQueryUsersAssignedEnrolmentsDelegatedPaye(empRef: EmpRef)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri =
        s"/enrolment-store-proxy/enrolment-store/enrolments/IR-PAYE~TaxOfficeNumber~${empRef.taxOfficeNumber}~TaxOfficeReference~${empRef.taxOfficeReference}/users\\?type=delegated"
    ).thenReturn(status, body)

  def stubQueryUsersAssignedEnrolmentsPrincipalPaye(saUtr: SaUtr)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/enrolment-store-proxy/enrolment-store/enrolments/IR-SA~UTR~${saUtr.value}/users\\?type=principal"
    ).thenReturn(status, body)

}
