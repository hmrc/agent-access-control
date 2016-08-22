package uk.gov.hmrc.agentaccesscontrol.connectors

import java.net.URL
import javax.xml.parsers.SAXParserFactory

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}

import scala.concurrent.Future

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
    val XML = scala.xml.XML.withSAXParser(SAXParserFactory.newInstance().newSAXParser())

    val xml = XML.loadString(xmlString)
    val agentDetails = xml \ "AllocatedAgents" \ "AgentDetails"
    val agentCode = (agentDetails \ "AgentCode").head.text

    val credentials = (agentDetails \ "AssignedCredentials" \ "Credential").map { elem =>
      AssignedCredentials((elem \ "CredentialIdentifier").head.text)
    }
    Some(AgentDetails(agentCode, credentials))
  }


  def body(utr: SaUtr): String =
    <GsoAdminGetAssignedAgentsXmlInput xmlns="urn:GSO-System-Services:external:2.13.3:GsoAdminGetAssignedAgentsXmlInput">
      <DelegatedAccessIdentifier>HMRC</DelegatedAccessIdentifier>
      <ServiceName>IR-SA</ServiceName>
      <Identifiers><Identifier IdentifierType="utr">{ utr }</Identifier></Identifiers>
    </GsoAdminGetAssignedAgentsXmlInput>.toString()
}
