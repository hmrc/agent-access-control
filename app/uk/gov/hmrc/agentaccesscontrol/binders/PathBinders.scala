/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.{AgentCode, EmpRef, Nino, SaUtr}

object PathBinders {
  implicit object AgentCodeBinder
      extends SimpleObjectBinder[AgentCode](AgentCode.apply, _.value)

  implicit object SaUtrBinder
      extends SimpleObjectBinder[SaUtr](SaUtr.apply, _.value)

  implicit object MtdItIdBinder
      extends SimpleObjectBinder[MtdItId](MtdItId.apply, _.value)

  implicit object NinoBinder
      extends SimpleObjectBinder[Nino](Nino.apply, _.value)

  implicit object VrnBinder extends SimpleObjectBinder[Vrn](Vrn.apply, _.value)

  implicit object UtrBinder extends SimpleObjectBinder[Utr](Utr.apply, _.value)

  implicit object TrustTaxIdentifierBinder
      extends SimpleObjectBinder[TrustTaxIdentifier](trustTax, _.value)

  private val urnPattern = "^((?i)[a-z]{2}trust[0-9]{8})$"
  private val utrPattern = "^\\d{10}$"

  def trustTax(id: String) = id match {
    case x if x.matches(utrPattern) => Utr(x)
    case x if x.matches(urnPattern) => Urn(x)
    case e                          => throw new Exception(s"invalid trust tax identifier $e")
  }

  implicit object CgtBinder
      extends SimpleObjectBinder[CgtRef](CgtRef.apply, _.value)

  implicit object EmpRefBinder extends PathBindable[EmpRef] {

    def bind(key: String, value: String) =
      try {
        Right(EmpRef.fromIdentifiers(value))
      } catch {
        case e: IllegalArgumentException =>
          Left(s"Cannot parse parameter '$key' with value '$value' as EmpRef")
      }

    def unbind(key: String, empRef: EmpRef): String = empRef.encodedValue
  }
}
