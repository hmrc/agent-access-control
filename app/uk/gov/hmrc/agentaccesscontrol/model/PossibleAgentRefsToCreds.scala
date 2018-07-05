package uk.gov.hmrc.agentaccesscontrol.model

import uk.gov.hmrc.agentaccesscontrol.connectors.AssignedAgentCredentials
import uk.gov.hmrc.domain.SaAgentReference

case class PossibleAgentRefsToCreds(saAgentReference: SaAgentReference, assignedAgentCredentials: AssignedAgentCredentials)
