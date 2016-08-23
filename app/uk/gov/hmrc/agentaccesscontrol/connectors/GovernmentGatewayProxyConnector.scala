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

package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

import org.apache.xerces.impl.Constants
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}

import scala.concurrent.Future
import scala.xml.Elem

case class AgentDetails(agentCode: String,
                        assignedCredentials: Seq[AssignedCredentials])

case class AssignedCredentials(identifier: String)

class GovernmentGatewayProxyConnector(baseUrl: URL, httpPost: HttpPost) {
  val url: URL = new URL(baseUrl, "/government-gateway-proxy/api/admin/GsoAdminGetAssignedAgents")

  def getAssignedSaAgents(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Option[AgentDetails]] = {
    httpPost.POSTString(url.toString, body(utr))
      .map(r => parseResponse(r.body))
  }

  def parseResponse(xmlString: String) : Option[AgentDetails] = {
    val xml: Elem = toXmlElement(xmlString)
    val agentDetails = xml \ "AllocatedAgents" \ "AgentDetails"
    if (agentDetails.nonEmpty) {
      val agentCode = (agentDetails \ "AgentCode").head.text

      val credentials = (agentDetails \ "AssignedCredentials" \ "Credential").map { elem =>
        AssignedCredentials((elem \ "CredentialIdentifier").head.text)
      }
      Some(AgentDetails(agentCode, credentials))
    } else {
      None
    }
  }


  def toXmlElement(xmlString: String): Elem = {
    val factory = SAXParserFactory.newInstance("org.apache.xerces.jaxp.SAXParserFactoryImpl", this.getClass.getClassLoader)
      factory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false)
      factory.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false)
      factory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE, true)
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    val XML = scala.xml.XML.withSAXParser(factory.newSAXParser())

    XML.loadString(xmlString)
  }

  def body(utr: SaUtr): String =
    <GsoAdminGetAssignedAgentsXmlInput xmlns="urn:GSO-System-Services:external:2.13.3:GsoAdminGetAssignedAgentsXmlInput">
      <DelegatedAccessIdentifier>HMRC</DelegatedAccessIdentifier>
      <ServiceName>IR-SA</ServiceName>
      <Identifiers><Identifier IdentifierType="utr">{ utr }</Identifier></Identifiers>
    </GsoAdminGetAssignedAgentsXmlInput>.toString()
}
