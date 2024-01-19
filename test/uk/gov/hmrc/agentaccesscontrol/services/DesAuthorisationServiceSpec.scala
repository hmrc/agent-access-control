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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentaccesscontrol.services
//
//import play.api.test.Helpers.await
//import uk.gov.hmrc.agentaccesscontrol.helpers.UnitSpec
//import uk.gov.hmrc.agentaccesscontrol.mocks.connectors.MockDesAgentClientApiConnector
//import uk.gov.hmrc.agentaccesscontrol.models.{
//  PayeFoundResponse,
//  PayeNotFoundResponse,
//  SaFoundResponse,
//  SaNotFoundResponse
//}
//import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
//import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//class DesAuthorisationServiceSpec
//    extends UnitSpec
//    with MockDesAgentClientApiConnector {
//
//  private val service = new DesAuthorisationService(
//    mockDesAgentClientApiConnector)
//  private val agentCode = AgentCode("ABCDEF123456")
//  private val saAgentRef = SaAgentReference("ABC456")
//  private val clientSaUtr = SaUtr("CLIENTSAUTR456")
//  private val empRef = EmpRef("123", "45676890")
//
//  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
//
//  "isAuthorisedInCesa" should {
//    "return false if the Agent or the relationship between the Agent and Client was not found in DES" in {
//      mockGetSaAgentClientRelationship(saAgentRef,
//                                       clientSaUtr,
//                                       Future.successful(SaNotFoundResponse))
//
//      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) mustBe false
//    }
//
//    "return true if the DES API returns 64-8=true and i64-8=true" in {
//      mockGetSaAgentClientRelationship(
//        saAgentRef,
//        clientSaUtr,
//        Future.successful(SaFoundResponse(auth64_8 = true, authI64_8 = true)))
//
//      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) mustBe true
//    }
//
//    "return false if the DES API returns 64-8=true and i64-8=false" in {
//      mockGetSaAgentClientRelationship(
//        saAgentRef,
//        clientSaUtr,
//        Future.successful(SaFoundResponse(auth64_8 = true, authI64_8 = false)))
//
//      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) mustBe false
//    }
//
//    "return false if the DES API returns 64-8=false and i64-8=true" in {
//      mockGetSaAgentClientRelationship(
//        saAgentRef,
//        clientSaUtr,
//        Future.successful(SaFoundResponse(auth64_8 = false, authI64_8 = true)))
//
//      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) mustBe false
//    }
//
//    "return false if the DES API returns 64-8=false and i64-8=false" in {
//      mockGetSaAgentClientRelationship(
//        saAgentRef,
//        clientSaUtr,
//        Future.successful(SaFoundResponse(auth64_8 = false, authI64_8 = false)))
//
//      await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr)) mustBe false
//    }
//
//    "propagate any errors that happened" in {
//      mockGetSaAgentClientRelationship(
//        saAgentRef,
//        clientSaUtr,
//        Future.failed(new BadRequestException("bad request")))
//
//      intercept[BadRequestException] {
//        await(service.isAuthorisedInCesa(agentCode, saAgentRef, clientSaUtr))
//      }
//    }
//  }
//
//  "isAuthorisedInEBS" should {
//    "return false if the relationship is not found in DES" in {
//      mockGetPayeAgentClientRelationship(
//        agentCode,
//        empRef,
//        Future.successful(PayeNotFoundResponse))
//
//      await(service.isAuthorisedInEbs(agentCode, empRef)) mustBe false
//    }
//
//    "return true if the 64-8=true" in {
//      mockGetPayeAgentClientRelationship(
//        agentCode,
//        empRef,
//        Future.successful(PayeFoundResponse(auth64_8 = true)))
//
//      await(service.isAuthorisedInEbs(agentCode, empRef)) mustBe true
//    }
//
//    "return false if the 64-8=false" in {
//      mockGetPayeAgentClientRelationship(
//        agentCode,
//        empRef,
//        Future.successful(PayeFoundResponse(auth64_8 = false)))
//
//      await(service.isAuthorisedInEbs(agentCode, empRef)) mustBe false
//    }
//
//    "propagate any errors that happened" in {
//      mockGetPayeAgentClientRelationship(
//        agentCode,
//        empRef,
//        Future.failed(new BadRequestException("bad request")))
//
//      intercept[BadRequestException] {
//        await(service.isAuthorisedInEbs(agentCode, empRef))
//      }
//    }
//
//  }
//
//}
