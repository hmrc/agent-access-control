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

package uk.gov.hmrc.agentaccesscontrol.mocks.connectors

import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.RelationshipsConnector
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockRelationshipsConnector extends MockFactory {

  protected val mockRelationshipsConnector: RelationshipsConnector =
    mock[RelationshipsConnector]

  def mockRelationshipExists(
      arn: Arn,
      userId: Option[String],
      taxId: TaxIdentifier,
      result: Future[Boolean]): CallHandler5[Arn,
                                             Option[String],
                                             TaxIdentifier,
                                             ExecutionContext,
                                             HeaderCarrier,
                                             Future[Boolean]] = {
    (mockRelationshipsConnector
      .relationshipExists(_: Arn, _: Option[String], _: TaxIdentifier)(
        _: ExecutionContext,
        _: HeaderCarrier))
      .expects(arn, userId, taxId, *, *)
      .atLeastOnce()
      .returning(result)
  }

}
