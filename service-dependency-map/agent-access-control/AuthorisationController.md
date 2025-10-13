# agent-access-control

## AuthorisationController

---

## GET /:authType/agent/:agentCode/client/:clientId

**Description:** Authorises an agent for a client based on the specified auth type. Supports multiple tax regimes including SA, PAYE, MTD-IT, MTD-VAT, Trust, CGT, PPT, CBC, and Pillar2.

### Sequence of Interactions

1. **API Call:** Authenticates the agent via `POST /auth/authorise` to the `auth` service and retrieves their credentials (ARN, SA Agent Reference, etc.).
2. **API Call:** Checks if the agent is suspended via `POST /agent-assurance/agent/verify-entity` to the `agent-assurance` service.
3. **API Call:** For SA/PAYE auth types, checks delegated users via `GET /enrolment-store/enrolments/{enrolmentKey}/users` to `enrolment-store-proxy`.
4. **API Call:** For SA auth, checks legacy SA relationship via `GET /sa/agents/{saAgentRef}/client/{saUtr}` to `des`.
5. **API Call:** For PAYE auth, checks legacy PAYE relationship via `GET /agents/regime/PAYE/agent/{agentCode}/client/{empRef}` to `des`.
6. **API Call:** For AFI auth, checks relationship via `GET /relationships/PERSONAL-INCOME-RECORD/agent/{arn}/client/{nino}` to `agent-fi-relationship`.
7. **API Call:** For standard services, checks modern relationships via `GET /agent/{arn}/service/{service}/client/{clientIdType}/{clientId}` to `agent-client-relationships`.
8. **API Call:** For standard services, falls back to DES via `GET /registration/relationship/arn/{arn}` if ACR check fails.
9. **API Call:** For SA with MTD agents, gets SA agent reference mapping via `GET /agent-mapping/mappings/sa/{arn}` to `agent-mapping`.
10. **API Call:** For standard services with granular permissions enabled, checks permissions via `GET /arn/{arn}/client/{clientId}` to `agent-permissions`.
11. **Audit Event:** Audits the access control decision with details of the outcome to `datastream`.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-access-control
    participant auth
    participant agent-assurance
    participant enrolment-store-proxy
    participant des
    participant agent-fi-relationship
    participant agent-client-relationships
    participant agent-mapping
    participant agent-permissions
    participant datastream

    Upstream->>+agent-access-control: GET /:authType/agent/:agentCode/client/:clientId
    agent-access-control->>+auth: POST /auth/authorise
    auth-->>-agent-access-control: Agent Credentials (ARN, SA Agent Ref, etc.)
    
    agent-access-control->>+agent-assurance: POST /agent-assurance/agent/verify-entity
    agent-assurance-->>-agent-access-control: Suspension Status
    
    Note over agent-access-control: Route based on authType
    
    alt SA Auth (sa-auth)
        agent-access-control->>+enrolment-store-proxy: GET /enrolment-store/enrolments/IR-SA~UTR~{saUtr}/users?type=delegated
        enrolment-store-proxy-->>-agent-access-control: Delegated Users
        agent-access-control->>+des: GET /sa/agents/{saAgentRef}/client/{saUtr}
        des-->>-agent-access-control: SA Relationship Status
        opt MTD Agent
            agent-access-control->>+agent-mapping: GET /agent-mapping/mappings/sa/{arn}
            agent-mapping-->>-agent-access-control: SA Agent Reference Mapping
        end
    else PAYE Auth (epaye-auth)
        agent-access-control->>+enrolment-store-proxy: GET /enrolment-store/enrolments/IR-PAYE~{empRef}/users?type=delegated
        enrolment-store-proxy-->>-agent-access-control: Delegated Users
        agent-access-control->>+des: GET /agents/regime/PAYE/agent/{agentCode}/client/{empRef}
        des-->>-agent-access-control: PAYE Relationship Status
    else AFI Auth (afi-auth)
        agent-access-control->>+agent-fi-relationship: GET /relationships/PERSONAL-INCOME-RECORD/agent/{arn}/client/{nino}
        agent-fi-relationship-->>-agent-access-control: AFI Relationship Status
    else Standard Service Auth (mtd-it-auth, mtd-vat-auth, trust-auth, cgt-auth, ppt-auth, cbc-auth, pillar2-auth)
        agent-access-control->>+agent-client-relationships: GET /agent/{arn}/service/{service}/client/{clientIdType}/{clientId}
        agent-client-relationships-->>-agent-access-control: Modern Relationship Status
        alt If ACR check fails
            agent-access-control->>+des: GET /registration/relationship/arn/{arn}
            des-->>-agent-access-control: Legacy Relationship Status
        end
        opt Granular Permissions Enabled
            agent-access-control->>+agent-permissions: GET /arn/{arn}/client/{clientId}
            agent-permissions-->>-agent-access-control: Permission Status
        end
    end
    
    agent-access-control->>datastream: Audit Event (AgentAccessControlDecision)
    agent-access-control-->>-Upstream: 200 OK | 401 Unauthorized
```

---

## POST /:authType/agent/:agentCode/client/:clientId

**Description:** Authorises an agent for a client based on the specified auth type. Identical functionality to GET endpoint but uses POST method. Supports multiple tax regimes including SA, PAYE, MTD-IT, MTD-VAT, Trust, CGT, PPT, CBC, and Pillar2.

### Sequence of Interactions

1. **API Call:** Authenticates the agent via `POST /auth/authorise` to the `auth` service and retrieves their credentials (ARN, SA Agent Reference, etc.).
2. **API Call:** Checks if the agent is suspended via `POST /agent-assurance/agent/verify-entity` to the `agent-assurance` service.
3. **API Call:** For SA/PAYE auth types, checks delegated users via `GET /enrolment-store/enrolments/{enrolmentKey}/users` to `enrolment-store-proxy`.
4. **API Call:** For SA auth, checks legacy SA relationship via `GET /sa/agents/{saAgentRef}/client/{saUtr}` to `des`.
5. **API Call:** For PAYE auth, checks legacy PAYE relationship via `GET /agents/regime/PAYE/agent/{agentCode}/client/{empRef}` to `des`.
6. **API Call:** For AFI auth, checks relationship via `GET /relationships/PERSONAL-INCOME-RECORD/agent/{arn}/client/{nino}` to `agent-fi-relationship`.
7. **API Call:** For standard services, checks modern relationships via `GET /agent/{arn}/service/{service}/client/{clientIdType}/{clientId}` to `agent-client-relationships`.
8. **API Call:** For standard services, falls back to DES via `GET /registration/relationship/arn/{arn}` if ACR check fails.
9. **API Call:** For SA with MTD agents, gets SA agent reference mapping via `GET /agent-mapping/mappings/sa/{arn}` to `agent-mapping`.
10. **API Call:** For standard services with granular permissions enabled, checks permissions via `GET /arn/{arn}/client/{clientId}` to `agent-permissions`.
11. **Audit Event:** Audits the access control decision with details of the outcome to `datastream`.

### Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Upstream
    participant agent-access-control
    participant auth
    participant agent-assurance
    participant enrolment-store-proxy
    participant des
    participant agent-fi-relationship
    participant agent-client-relationships
    participant agent-mapping
    participant agent-permissions
    participant datastream

    Upstream->>+agent-access-control: POST /:authType/agent/:agentCode/client/:clientId
    agent-access-control->>+auth: POST /auth/authorise
    auth-->>-agent-access-control: Agent Credentials (ARN, SA Agent Ref, etc.)
    
    agent-access-control->>+agent-assurance: POST /agent-assurance/agent/verify-entity
    agent-assurance-->>-agent-access-control: Suspension Status
    
    Note over agent-access-control: Route based on authType
    
    alt SA Auth (sa-auth)
        agent-access-control->>+enrolment-store-proxy: GET /enrolment-store/enrolments/IR-SA~UTR~{saUtr}/users?type=delegated
        enrolment-store-proxy-->>-agent-access-control: Delegated Users
        agent-access-control->>+des: GET /sa/agents/{saAgentRef}/client/{saUtr}
        des-->>-agent-access-control: SA Relationship Status
        opt MTD Agent
            agent-access-control->>+agent-mapping: GET /agent-mapping/mappings/sa/{arn}
            agent-mapping-->>-agent-access-control: SA Agent Reference Mapping
        end
    else PAYE Auth (epaye-auth)
        agent-access-control->>+enrolment-store-proxy: GET /enrolment-store/enrolments/IR-PAYE~{empRef}/users?type=delegated
        enrolment-store-proxy-->>-agent-access-control: Delegated Users
        agent-access-control->>+des: GET /agents/regime/PAYE/agent/{agentCode}/client/{empRef}
        des-->>-agent-access-control: PAYE Relationship Status
    else AFI Auth (afi-auth)
        agent-access-control->>+agent-fi-relationship: GET /relationships/PERSONAL-INCOME-RECORD/agent/{arn}/client/{nino}
        agent-fi-relationship-->>-agent-access-control: AFI Relationship Status
    else Standard Service Auth (mtd-it-auth, mtd-vat-auth, trust-auth, cgt-auth, ppt-auth, cbc-auth, pillar2-auth)
        agent-access-control->>+agent-client-relationships: GET /agent/{arn}/service/{service}/client/{clientIdType}/{clientId}
        agent-client-relationships-->>-agent-access-control: Modern Relationship Status
        alt If ACR check fails
            agent-access-control->>+des: GET /registration/relationship/arn/{arn}
            des-->>-agent-access-control: Legacy Relationship Status
        end
        opt Granular Permissions Enabled
            agent-access-control->>+agent-permissions: GET /arn/{arn}/client/{clientId}
            agent-permissions-->>-agent-access-control: Permission Status
        end
    end
    
    agent-access-control->>datastream: Audit Event (AgentAccessControlDecision)
    agent-access-control-->>-Upstream: 200 OK | 401 Unauthorized
```
