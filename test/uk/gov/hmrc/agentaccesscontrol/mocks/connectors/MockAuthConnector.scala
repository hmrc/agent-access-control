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

import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{Nino => _, _}

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthConnector extends MockFactory {

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  // We have 1 common way to authorise, not sure this is worth making input explicit
  def mockAuthorise(
      returnValue: Future[
        ~[~[~[Option[String], Enrolments], Option[CredentialRole]],
          Option[Credentials]]]): CallHandler4[
    Predicate,
    Retrieval[Option[String] ~ Enrolments ~ Option[CredentialRole] ~ Option[
      Credentials]],
    HeaderCarrier,
    ExecutionContext,
    Future[Option[String] ~ Enrolments ~ Option[CredentialRole] ~ Option[
      Credentials]]] = {
    (
      mockAuthConnector
        .authorise(_: Predicate,
                   _: Retrieval[~[~[~[Option[String], Enrolments],
                                    Option[CredentialRole]],
                                  Option[Credentials]]])(
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *)
      .returning(returnValue)
  }

}
