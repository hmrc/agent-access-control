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

import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.WsTestClient
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.ws.WSHttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class Resource(path: String)(port: Int) {

  def get(trueClientIp: Option[String] = None): HttpResponse = doGet(path)(HeaderCarrier(trueClientIp = trueClientIp))

  def post[A](trueClientIp: Option[String] = None, body: A)(implicit writes: Writes[A]): HttpResponse = {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier(trueClientIp = trueClientIp)
    doPost(path, body)
  }

  private def doGet(url: String)(implicit hc: HeaderCarrier): HttpResponse = perform(url) { _.get() }

  private def doPost[A](url: String, body: A, headers: Seq[(String, String)] = Seq.empty)(
    implicit writes: Writes[A],
    hc: HeaderCarrier): HttpResponse = perform(url) { _.post(Json.toJson(body)) }

  private def perform(url: String)(fun: WSRequest => Future[WSResponse])(implicit hc: HeaderCarrier): WSHttpResponse =
    await(
      fun(
        WsTestClient.wsUrl(url)(port)
          .withHeaders(hc.headers: _*)
          .withRequestTimeout(Duration(20, SECONDS)))
        .map(new WSHttpResponse(_)))

  private def await[A](future: Future[A]): A = Await.result(future, Duration(10, SECONDS))

}
