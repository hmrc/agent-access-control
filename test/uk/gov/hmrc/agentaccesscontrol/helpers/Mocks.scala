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

package uk.gov.hmrc.agentaccesscontrol.helpers

import org.mockito.IdiomaticMockito
import uk.gov.hmrc.agentaccesscontrol.audit.AuditService
import uk.gov.hmrc.agentaccesscontrol.connectors.desapi.DesAgentClientApiConnector
import uk.gov.hmrc.agentaccesscontrol.connectors.{
  AfiRelationshipConnector,
  AgentPermissionsConnector,
  EnrolmentStoreProxyConnector,
  MappingConnector
}
import uk.gov.hmrc.agentaccesscontrol.connectors.mtd.{
  AgentClientAuthorisationConnector,
  RelationshipsConnector
}
import uk.gov.hmrc.agentaccesscontrol.services.{
  AuthorisationService,
  DesAuthorisationService,
  ESAuthorisationService,
  EnrolmentStoreProxyAuthorisationService
}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

trait Mocks extends IdiomaticMockito {

  protected val mockACAConnector: AgentClientAuthorisationConnector =
    mock[AgentClientAuthorisationConnector]

  protected val mockAfiRelationshipConnector: AfiRelationshipConnector =
    mock[AfiRelationshipConnector]

  protected val mockAgentPermissionsConnector: AgentPermissionsConnector =
    mock[AgentPermissionsConnector]

  protected val mockAuditConnector: AuditConnector = mock[AuditConnector]

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  protected val mockDesAgentClientApiConnector: DesAgentClientApiConnector =
    mock[DesAgentClientApiConnector]

  protected val mockESPConnector: EnrolmentStoreProxyConnector =
    mock[EnrolmentStoreProxyConnector]

  protected val mockMappingConnector: MappingConnector = mock[MappingConnector]

  protected val mockRelationshipsConnector: RelationshipsConnector =
    mock[RelationshipsConnector]

  protected val mockAuditService: AuditService =
    mock[AuditService]

  protected val mockAuthorisationService: AuthorisationService =
    mock[AuthorisationService]

  protected val mockDesAuthorisationService: DesAuthorisationService =
    mock[DesAuthorisationService]

  protected val mockESAuthorisationService: ESAuthorisationService =
    mock[ESAuthorisationService]

  protected val mockESPAuthorisationService
    : EnrolmentStoreProxyAuthorisationService =
    mock[EnrolmentStoreProxyAuthorisationService]

}
