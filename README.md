# Market Basket Platform

Market Basket Platform is a Spring Boot microservice system for a grocery subscription marketplace. The repository currently contains eight independently buildable services, shared local infrastructure through Docker Compose, Kong Gateway for local edge routing, Prometheus/Grafana/Alertmanager observability, local SonarQube, and GitHub Actions workflows for CI, container publication, migration-gated deployment, and smoke checks.

## Services

| Service | Port | Current role |
| --- | ---: | --- |
| `auth-service` | 8080 | User registration, login, JWT access tokens, refresh-token rotation, logout, admin RBAC, Google OAuth2 entry points, JWKS publication, and auth outbox publishing. |
| `customer-service` | 8081 | Service-owned customer profiles keyed by auth user id, self-service profile APIs, support/admin read APIs, and idempotent auth registration event consumption. |
| `seller-service` | 8087 | Seller store creation, approval/rejection workflow, owner/staff memberships, JWT role gates, and seller event contracts. |
| `catalog-service` | 8082 | Category and seller-owned product management with draft/published/unpublished lifecycle, public reads, JWT role gates, and catalog event contracts. |
| `subscription-service` | 8083 | Subscription bounded context scaffold. |
| `order-service` | 8084 | Order bounded context scaffold. |
| `inventory-service` | 8085 | Seller stock upsert/list/read, active reservation/release workflow, JWT role gates, and inventory event contracts. |
| `notification-service` | 8086 | Notification bounded context scaffold. |

Each service is a Java 17 Spring Boot application with Maven wrapper support, Spotless formatting, Actuator, JPA, Flyway, Redis, Kafka, validation, Web MVC, PostgreSQL, Prometheus metrics, Sentry wiring, and Testcontainers-based test dependencies. The auth service also includes Spring Security, OAuth2 resource server/client support, JWT infrastructure, password hashing, refresh tokens, and a Kafka outbox publisher. Customer, seller, catalog, and inventory include OAuth2 resource-server JWT validation against auth-service JWKS.

## Local Quick Start

Prerequisites:

- Docker and Docker Compose
- JDK 17

Run the full local stack:

```bash
docker compose up --build
```

Run one service's checks:

```bash
cd services/auth-service
./mvnw -B -ntp spotless:check package
```

Schema changes are managed with versioned Flyway migrations under each service's `src/main/resources/db/migration` directory. Flyway runs when services start, and CI validates every service migration set against PostgreSQL.

Useful local endpoints:

- Auth health: `http://localhost:8080/actuator/health`
- Auth JWKS: `http://localhost:8080/.well-known/jwks.json`
- Kong Gateway: `http://localhost:8000`
- Auth JWKS through Kong: `http://localhost:8000/.well-known/jwks.json`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Alertmanager: `http://localhost:9093`
- SonarQube: `http://localhost:9000`

## Documentation

- [Documentation index](docs/README.md)
- [Product requirements](docs/prd.md)
- [Architecture](docs/architecture.md)
- [Diagrams](docs/diagrams.md)
- [API reference](docs/api.md)
- [Local development](docs/local-development.md)
- [CI/CD flow](docs/cicd.md)
- [Operations](docs/operations.md)
- [Auth service TDD plan](docs/auth-service-tdd-plan.md)

## CI/CD Summary

Pull requests and pushes to `main` run migration validation, formatting, tests, and packaging across all services. Pushes to `main` also build and publish Docker images to GitHub Container Registry. A successful image publication triggers the dev deployment workflow with the published Git SHA image tag, while production deployment is manual through the `Deploy Prod` workflow and the protected `prod` environment.
