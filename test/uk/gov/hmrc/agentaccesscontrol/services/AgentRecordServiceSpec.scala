/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentaccesscontrol.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentaccesscontrol.models.SuspensionDetails
import uk.gov.hmrc.http.HeaderCarrier

class AgentRecordServiceSpec extends PlaySpec with IdiomaticMockito {

  trait Setup {
    protected val mockAgentServicesAccountConnector: AgentServicesAccountConnector =
      mock[AgentServicesAccountConnector]

    object TestService
        extends AgentRecordService(
          mockAgentServicesAccountConnector
        )
  }

  val suspensionDetails: SuspensionDetails = SuspensionDetails(suspensionStatus = false, None)

  implicit val ec: ExecutionContext = mock[ExecutionContext]
  implicit val hc: HeaderCarrier    = mock[HeaderCarrier]

  "AgentRecordService" should {

    "Get agent suspension status from agent services account" in new Setup {

      mockAgentServicesAccountConnector
        .getSuspensionDetails(*[HeaderCarrier])
        .returns(Future.successful(suspensionDetails))

      val result = TestService.getAgentRecord.futureValue

      result mustBe suspensionDetails

    }
  }
}
