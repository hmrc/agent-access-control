/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentaccesscontrol.utils

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Writes

trait WiremockMethods {
  def when[T](method: HTTPMethod, uri: String, body: T)(implicit writes: Writes[T]): Mapping = {
    when(method, uri, Map.empty, body)
  }

  def when(method: HTTPMethod, uri: String, headers: Map[String, String] = Map.empty): Mapping = {
    new Mapping(method, uri, headers, None)
  }

  def when[T](method: HTTPMethod, uri: String, headers: Map[String, String], body: T)(
      implicit writes: Writes[T]
  ): Mapping = {
    val stringBody = writes.writes(body).toString()
    new Mapping(method, uri, headers, Some(stringBody))
  }
  class Mapping(
      method: HTTPMethod,
      uri: String,
      headers: Map[String, String],
      body: Option[String]
  ) {
    private val mapping = {
      val uriMapping = method.wireMockMapping(urlMatching(uri))

      val uriMappingWithHeaders = headers.foldLeft(uriMapping) {
        case (m, (key, value)) => m.withHeader(key, equalTo(value))
      }

      body match {
        case Some(extractedBody) => uriMappingWithHeaders.withRequestBody(equalToJson(extractedBody))
        case None                => uriMappingWithHeaders
      }
    }

    def thenReturn[T](status: Int, body: T)(implicit writes: Writes[T]): StubMapping = {
      val stringBody = writes.writes(body).toString()
      thenReturnInternal(status, Map.empty, Some(stringBody))
    }

    def thenReturn[T](status: Int, headers: Map[String, String], body: T)(implicit writes: Writes[T]): StubMapping = {
      val stringBody = writes.writes(body).toString()
      thenReturnInternal(status, headers, Some(stringBody))
    }

    def thenReturn(status: Int, headers: Map[String, String] = Map.empty): StubMapping = {
      thenReturnInternal(status, headers, None)
    }

    private def thenReturnInternal(status: Int, headers: Map[String, String], body: Option[String]): StubMapping = {
      val response = {
        val statusResponse = aResponse().withStatus(status)
        val responseWithHeaders = headers.foldLeft(statusResponse) {
          case (res, (key, value)) => res.withHeader(key, value)
        }
        body match {
          case Some(extractedBody) => responseWithHeaders.withBody(extractedBody)
          case None                => responseWithHeaders
        }
      }

      stubFor(mapping.willReturn(response))
    }
  }

  sealed trait HTTPMethod {
    val wireMockMapping: UrlPattern => MappingBuilder
  }

  case object GET extends HTTPMethod {
    override val wireMockMapping: UrlPattern => MappingBuilder = get
  }

  case object POST extends HTTPMethod {
    override val wireMockMapping: UrlPattern => MappingBuilder = post
  }

  case object PUT extends HTTPMethod {
    override val wireMockMapping: UrlPattern => MappingBuilder = put
  }

}
