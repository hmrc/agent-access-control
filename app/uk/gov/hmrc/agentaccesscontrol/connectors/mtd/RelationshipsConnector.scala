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

package uk.gov.hmrc.agentaccesscontrol.connectors.mtd

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import play.api.libs.json._
import uk.gov.hmrc.agentaccesscontrol.model.{Arn, MtdClientId}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

case class Relationship(arn: String, clientId: String)
object Relationship {
  implicit val jsonReads = Json.reads[Relationship]
}

@Singleton
class RelationshipsConnector @Inject() (@Named("agent-client-relationships-baseUrl") baseUrl: URL, httpGet: HttpGet) {

  def fetchRelationship(arn: Arn, mtdSaClientId: MtdClientId)
                       (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Relationship]] = {
    httpGet.GET[Option[Relationship]](relationshipUrl(arn, mtdSaClientId).toString)
  }

  private def relationshipUrl(arn: Arn, mtdSaClientId: MtdClientId) =
          new URL(baseUrl, s"/agent-client-relationships/relationships/mtd-sa/${mtdSaClientId.value}/${arn.value}")
}
