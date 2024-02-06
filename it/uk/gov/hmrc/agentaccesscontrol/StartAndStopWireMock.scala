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

package uk.gov.hmrc.agentaccesscontrol

import java.net.ServerSocket

import scala.annotation.tailrec

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEachTestData
import org.scalatest.Suite
import org.scalatest.TestData
import play.api.Logging

object StartAndStopWireMock {
  // We have to make the wireMockPort constant per-JVM instead of constant
  // per-WireMockSupport-instance because config values containing it are
  // cached in the GGConfig object
  private lazy val wireMockPort = Port.randomAvailable
}

trait StartAndStopWireMock extends BeforeAndAfterEachTestData with BeforeAndAfterAll {
  self: Suite =>

  protected val wiremockPort            = StartAndStopWireMock.wireMockPort
  protected val wiremockHost            = "localhost"
  protected val wiremockBaseUrl: String = s"http://$wiremockHost:$wiremockPort"
  val wireMockServer                    = new WireMockServer(wireMockConfig().port(wiremockPort))

  override def beforeAll() = {
    wireMockServer.stop()
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  override def beforeEach(testData: TestData) =
    WireMock.reset()

  protected override def afterAll(): Unit =
    wireMockServer.stop()
}

// This class was copy-pasted from the hmrctest project, which is now deprecated.
object Port extends Logging {
  val rnd       = new scala.util.Random
  val range     = 8000 to 39999
  val usedPorts = List[Int]()

  @tailrec
  def randomAvailable: Int =
    range(rnd.nextInt(range.length)) match {
      case 8080 => randomAvailable
      case 8090 => randomAvailable
      case p: Int => {
        available(p) match {
          case false => {
            logger.debug(s"Port $p is in use, trying another")
            randomAvailable
          }
          case true => {
            logger.debug("Taking port : " + p)
            usedPorts :+ p
            p
          }
        }
      }
    }

  private def available(p: Int): Boolean = {
    var socket: ServerSocket = null
    try {
      if (!usedPorts.contains(p)) {
        socket = new ServerSocket(p)
        socket.setReuseAddress(true)
        true
      } else {
        false
      }
    } catch {
      case t: Throwable => false
    } finally {
      if (socket != null) socket.close()
    }
  }
}
