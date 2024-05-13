package uk.gov.hmrc.agentaccesscontrol.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockHelper
import uk.gov.hmrc.agentaccesscontrol.utils.WiremockMethods
import uk.gov.hmrc.agentmtdidentifiers.model._

trait AgentClientRelationshipStub extends WiremockMethods {

  def stubMtdItAgentClientRelationship(arn: Arn, mtdItId: MtdItId)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${mtdItId.value}"
    ).thenReturn(status)

  def stubMtdVatAgentClientRelationship(arn: Arn, vrn: Vrn)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/${vrn.value}"
    ).thenReturn(status)

  def stubCgtAgentClientRelationship(arn: Arn, cgtRef: CgtRef)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}"
    ).thenReturn(status)

  def stubPptAgentClientRelationship(arn: Arn, pptRef: PptRef)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-PPT-ORG/client/EtmpRegistrationNumber/${pptRef.value}"
    ).thenReturn(status)

  def stubCbcIdAgentClientRelationship(arn: Arn, cbcId: CbcId)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CBC-ORG/client/cbcId/${cbcId.value}"
    ).thenReturn(status)

  def stubPlrIdAgentClientRelationship(arn: Arn, PlrId: PlrId)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-PILLAR2-ORG/client/PLRID/${PlrId.value}"
    ).thenReturn(status)

  def stubTersAgentClientRelationship(arn: Arn, utr: Utr)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERS-ORG/client/SAUTR/${utr.value}"
    ).thenReturn(status)

  def stubTersntAgentClientRelationship(arn: Arn, urn: Urn)(status: Int): StubMapping =
    when(
      method = GET,
      uri = s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERSNT-ORG/client/URN/${urn.value}"
    ).thenReturn(status)

  def stubMtdItAgentClientRelationshipToUser(arn: Arn, mtdItId: MtdItId, providerId: String)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${mtdItId.value}\\?userId=$providerId"
    ).thenReturn(status)

  def stubMtdVatAgentClientRelationshipToUser(arn: Arn, vrn: Vrn, providerId: String)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/${vrn.value}\\?userId=$providerId"
    ).thenReturn(status)
  def stubCgtAgentClientRelationshipToUser(arn: Arn, cgtRef: CgtRef, providerId: String)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}\\?userId=$providerId"
    ).thenReturn(status)

  def stubPptAgentClientRelationshipToUser(arn: Arn, pptRef: PptRef, providerId: String)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-PPT-ORG/client/EtmpRegistrationNumber/${pptRef.value}\\?userId=$providerId"
    ).thenReturn(status)

  def stubTersAgentClientRelationshipToUser(arn: Arn, utr: Utr, providerId: String)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERS-ORG/client/SAUTR/${utr.value}\\?userId=$providerId"
    ).thenReturn(status)

  def stubTersntAgentClientRelationshipToUser(arn: Arn, urn: Urn, providerId: String)(status: Int): StubMapping =
    when(
      method = GET,
      uri =
        s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERSNT-ORG/client/URN/${urn.value}\\?userId=$providerId"
    ).thenReturn(status)

  def verifyCgtAgentClientRelationshipToUser(arn: Arn, cgtRef: CgtRef, providerId: String)(timesCalled: Int): Unit =
    WiremockHelper.verifyGet(
      timesCalled,
      s"/agent-client-relationships/agent/${arn.value}/service/HMRC-CGT-PD/client/CGTPDRef/${cgtRef.value}?userId=$providerId"
    )

}
