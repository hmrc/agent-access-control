/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.models

enum AccessResponse {

  /**
   * Represents a response where it was verified that the agent user is authorised to act for a given client.
   */
  case Authorised

  /**
   * Represents a response where the agent user cannot act for a given client because the agency has no relationship with the client.
   */
  case NoRelationship

  /**
   * Represents a response where the agent user cannot act for a given client because the agency has a relationship with the client
   * but the agent user has not been assigned the client (when access groups/granular permissions are enabled).
   */
  case NoAssignment

  /**
   * Represents a response where the agent user cannot act for a given client because the agency is suspended.
   */
  case AgentSuspended

  /**
   * Represents a response where the agent user cannot act for a given client because we cannot verify it for some other reason.
   */
  case Error(message: String)

  /**
   * For our own benefit we have more fine-grained responses, but auth will only handle NoAssignment and NoRelationship
   * responses so we intentionally convert other responses into 'NoRelationship'.
   */
}

object AccessResponse {

  def toReason(accessResponse: AccessResponse): Seq[(String, Any)] =
    accessResponse match {
      case AccessResponse.Authorised   => Seq.empty[(String, Any)]
      case AccessResponse.NoAssignment => Seq(("reason", "NoAssignment"))
      case _ =>
        Seq(("reason", "NoRelationship"))
    }
}
