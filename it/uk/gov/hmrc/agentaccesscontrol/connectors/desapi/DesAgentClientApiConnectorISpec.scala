///*
// * Copyright 2016 HM Revenue & Customs
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
//package uk.gov.hmrc.agentaccesscontrol.connectors.desapi
//
//import java.net.URL
//
//import akka.actor.ActorSystem
//import com.codahale.metrics.MetricRegistry
//import com.kenshoo.play.metrics.Metrics
//import org.mockito.ArgumentMatchers.{any, eq => eqs}
//import org.mockito.Mockito.when
//import org.scalatest.Assertion
//import org.scalatestplus.mockito.MockitoSugar
//import play.api.Configuration
//import play.api.libs.json.Json
//import uk.gov.hmrc.agentaccesscontrol.model._
//import uk.gov.hmrc.agentaccesscontrol.module.HttpVerbs
//import uk.gov.hmrc.agentaccesscontrol.support.{MetricTestSupportAppPerSuite, WireMockWithOneAppPerSuiteISpec}
//import uk.gov.hmrc.domain.{AgentCode, EmpRef, SaAgentReference, SaUtr}
//import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
//import uk.gov.hmrc.play.audit.model.MergedDataEvent
//
//import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
//
//class DesAgentClientApiConnectorISpec
//    extends WireMockWithOneAppPerSuiteISpec
//    with MockitoSugar
//    with MetricTestSupportAppPerSuite {
//
//  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
//  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
//  val httpVerbs: HttpVerbs = app.injector.instanceOf[HttpVerbs]
//
//  "getSaAgentClientRelationship" should {
//    "request DES API with the correct auth tokens" in new Context {
//      givenClientIsLoggedIn()
//        .andIsRelatedToSaClientInDes(saUtr, "auth_token_33", "env_33")
//        .andAuthorisedByBoth648AndI648()
//      givenCleanMetricRegistry()
//
//      val connectorWithDifferentHeaders = new DesAgentClientApiConnector(
//        new URL(wiremockBaseUrl),
//        "auth_token_33",
//        "env_33",
//        httpVerbs,
//        app.injector.instanceOf[Metrics])
//
//      val response = await(connectorWithDifferentHeaders.getSaAgentClientRelationship(saAgentReference, saUtr))
//      response shouldBe SaFoundResponse(auth64_8 = true, authI64_8 = true)
//      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetSaAgentClientRelationship-GET")
//    }
//
//    "pass along 64-8 and i64-8 information" when {
//      "agent is authorised by 64-8 and i64-8" in new Context {
//        givenClientIsLoggedIn()
//          .andIsRelatedToSaClientInDes(saUtr)
//          .andAuthorisedByBoth648AndI648()
//
//        when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext]))
//          .thenThrow(new RuntimeException("EXCEPTION!"))
//
//        await(connector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
//          auth64_8 = true,
//          authI64_8 = true)
//      }
//      "agent is authorised by only i64-8" in new Context {
//        givenClientIsLoggedIn()
//          .andIsRelatedToSaClientInDes(saUtr)
//          .andIsAuthorisedByOnlyI648()
//
//        await(connector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
//          auth64_8 = false,
//          authI64_8 = true)
//      }
//      "agent is authorised by only 64-8" in new Context {
//        givenClientIsLoggedIn()
//          .andIsRelatedToSaClientInDes(saUtr)
//          .andIsAuthorisedByOnly648()
//
//        await(connector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
//          auth64_8 = true,
//          authI64_8 = false)
//      }
//      "agent is not authorised" in new Context {
//        givenClientIsLoggedIn()
//          .andIsRelatedToSaClientInDes(saUtr)
//          .butIsNotAuthorised()
//
//        await(connector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
//          auth64_8 = false,
//          authI64_8 = false)
//      }
//    }
//
//    "return NotFoundResponse in case of a 404" in new Context {
//      givenClientIsLoggedIn()
//        .andHasNoRelationInDesWith(saUtr)
//
//      await(connector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaNotFoundResponse
//    }
//
//    "fail in any other cases, like internal server error" in new Context {
//      givenClientIsLoggedIn().andDesIsDown()
//
//      an[Exception] should be thrownBy await(connector.getSaAgentClientRelationship(saAgentReference, saUtr))
//    }
//
//    "log metrics for the outbound call" in new Context {
//      givenClientIsLoggedIn()
//        .andIsRelatedToSaClientInDes(saUtr)
//        .andAuthorisedByBoth648AndI648()
//      givenCleanMetricRegistry()
//
//      await(connector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
//        auth64_8 = true,
//        authI64_8 = true)
//      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetSaAgentClientRelationship-GET")
//    }
//
//    "audit outbound DES call" in new Context {
//      givenClientIsLoggedIn()
//        .andIsRelatedToSaClientInDes(saUtr)
//        .andAuthorisedByBoth648AndI648()
//
//      when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext]))
//        .thenThrow(new RuntimeException("EXCEPTION!"))
//
//      await(auditConnector.getSaAgentClientRelationship(saAgentReference, saUtr)) shouldBe SaFoundResponse(
//        auth64_8 = true,
//        authI64_8 = true)
//      outboundSaCallToDesShouldBeAudited(auth64_8 = true, authI64_8 = true)
//    }
//  }
//
//  "getPayeAgentClientRelationship" should {
//    "request DES API with the correct auth tokens" in new Context {
//      givenClientIsLoggedIn()
//        .andIsRelatedToPayeClientInDes(empRef, "auth_token_33", "env_33")
//        .andIsAuthorisedBy648()
//      givenCleanMetricRegistry()
//
//      val connectorWithDifferentHeaders = new DesAgentClientApiConnector(
//        new URL(wiremockBaseUrl),
//        "auth_token_33",
//        "env_33",
//        httpVerbs,
//        app.injector.instanceOf[Metrics])
//
//      val response = await(connectorWithDifferentHeaders.getPayeAgentClientRelationship(agentCode, empRef))
//      response shouldBe PayeFoundResponse(auth64_8 = true)
//      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetPayeAgentClientRelationship-GET")
//    }
//
//    "pass along 64-8 and i64-8 information" when {
//      "agent is authorised by 64-8" in new Context {
//        givenClientIsLoggedIn()
//          .andIsRelatedToPayeClientInDes(empRef)
//          .andIsAuthorisedBy648()
//
//        await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true)
//      }
//      "agent is not authorised" in new Context {
//        givenClientIsLoggedIn()
//          .andIsRelatedToPayeClientInDes(empRef)
//          .butIsNotAuthorised()
//
//        await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = false)
//      }
//    }
//
//    "return NotFoundResponse in case of a 404" in new Context {
//      givenClientIsLoggedIn()
//        .andHasNoRelationInDesWith(empRef)
//
//      await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeNotFoundResponse
//    }
//
//    "fail in any other cases, like internal server error" in new Context {
//      givenClientIsLoggedIn().andDesIsDown()
//
//      an[Exception] should be thrownBy await(connector.getPayeAgentClientRelationship(agentCode, empRef))
//    }
//
//    "log metrics for outbound call" in new Context {
//      givenClientIsLoggedIn()
//        .andIsRelatedToPayeClientInDes(empRef)
//        .andIsAuthorisedBy648()
//      givenCleanMetricRegistry()
//
//      await(connector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(auth64_8 = true)
//      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetPayeAgentClientRelationship-GET")
//    }
//
//    "audit outbound call to DES" in new Context {
//      givenClientIsLoggedIn()
//        .andIsRelatedToPayeClientInDes(empRef)
//        .andIsAuthorisedBy648()
//
//      when(mockAuditConnector.sendMergedEvent(any[MergedDataEvent])(eqs(headerCarrier), any[ExecutionContext]))
//        .thenThrow(new RuntimeException("EXCEPTION!"))
//
//      await(auditConnector.getPayeAgentClientRelationship(agentCode, empRef)) shouldBe PayeFoundResponse(
//        auth64_8 = true)
//      outboundPayeCallToDesShouldBeAudited(auth64_8 = true)
//    }
//  }
//
//  private abstract class Context extends MockAuditingContext {
//
//    val httpVerbsNew = new HttpVerbs(mockAuditConnector, "", app.injector.instanceOf[Configuration], app.injector.instanceOf[ActorSystem])
//    val auditConnector =
//      new DesAgentClientApiConnector(new URL(wiremockBaseUrl), "secret", "test", httpVerbsNew, FakeMetrics)
//    val connector = new DesAgentClientApiConnector(
//      new URL(wiremockBaseUrl),
//      "secret",
//      "test",
//      httpVerbs,
//      app.injector.instanceOf[Metrics])
//    val saAgentReference = SaAgentReference("AGENTR")
//    val saUtr = SaUtr("SAUTR456")
//    val empRef = EmpRef("123", "4567890")
//    val agentCode = AgentCode("A1234567890A")
//
//    def givenClientIsLoggedIn() =
//      given()
//        .agentAdmin(agentCode.value)
//        .isLoggedIn()
//        .andHasSaAgentReferenceWithEnrolment(saAgentReference)
//
//    def outboundSaCallToDesShouldBeAudited(auth64_8: Boolean, authI64_8: Boolean): Assertion = {
//      val event: MergedDataEvent = capturedEvent()
//
//      event.auditType shouldBe "OutboundCall"
//
//      event.request.tags("path") shouldBe s"$wiremockBaseUrl/sa/agents/$saAgentReference/client/$saUtr"
//
//      val responseJson = Json.parse(event.response.detail("responseMessage"))
//      (responseJson \ "Auth_64-8").as[Boolean] shouldBe auth64_8
//      (responseJson \ "Auth_i64-8").as[Boolean] shouldBe authI64_8
//    }
//
//    def outboundPayeCallToDesShouldBeAudited(auth64_8: Boolean): Assertion = {
//      val event: MergedDataEvent = capturedEvent()
//
//      event.auditType shouldBe "OutboundCall"
//
//      event.request.tags("path") shouldBe s"$wiremockBaseUrl/agents/regime/PAYE/agent/$agentCode/client/${empRef.taxOfficeNumber}${empRef.taxOfficeReference}"
//
//      val responseJson = Json.parse(event.response.detail("responseMessage"))
//      (responseJson \ "Auth_64-8").as[Boolean] shouldBe auth64_8
//    }
//  }
//}
//
//object FakeMetrics extends Metrics {
//  override def defaultRegistry: MetricRegistry = new MetricRegistry
//  override def toJson: String = ???
//}
