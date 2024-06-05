/*
 * Copyright 2024 HM Revenue & Customs
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
