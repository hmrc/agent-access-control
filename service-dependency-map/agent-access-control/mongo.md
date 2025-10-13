# Analysis of MongoDB Collections in agent-access-control

## No MongoDB Collections Found

The `agent-access-control` microservice does not use MongoDB for data persistence. This service is designed as a **stateless orchestration layer** that provides authorization decisions by calling various downstream services.

### Service Architecture

This microservice follows a **stateless design pattern** where:

- **No local data storage**: All data is retrieved from downstream services
- **Orchestration role**: Coordinates calls to multiple backend services to make authorization decisions
- **Caching strategy**: May use in-memory caching but does not persist data

### Data Sources

Instead of local MongoDB collections, this service retrieves data from:

1. **enrolment-store-proxy**: Client enrolment information
2. **des**: Legacy agent-client relationships
3. **agent-client-relationships**: Modern agent-client relationships
4. **agent-fi-relationship**: Agent relationships for personal income records
5. **agent-mapping**: SA agent reference mappings
6. **agent-permissions**: Granular permission checks
7. **agent-assurance**: Agent suspension status

### Implications

- **No database migrations** required for this service
- **No data backup/restore** procedures needed
- **Stateless scaling** possible without data consistency concerns
- **Service dependencies** are critical for functionality
