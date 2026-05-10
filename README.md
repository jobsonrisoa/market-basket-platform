# Market Basket Platform

Market Basket Platform is a Spring Boot microservice system for a grocery or market commerce domain. The repository currently contains eight independently buildable services, shared local infrastructure through Docker Compose, Kong Gateway for local edge routing, and GitHub Actions workflows for CI, container publication, and environment deployments.

## Services

| Service | Port | Current role |
| --- | ---: | --- |
| `auth-service` | 8080 | User registration, login, JWT access tokens, refresh-token rotation, logout, Google OAuth2 entry points, and JWKS publication. |
| `customer-service` | 8081 | Customer bounded context scaffold. |
| `seller-service` | 8087 | Seller onboarding, store profile, staff membership, and seller operations scaffold. |
| `catalog-service` | 8082 | Product catalog bounded context scaffold. |
| `subscription-service` | 8083 | Subscription bounded context scaffold. |
| `order-service` | 8084 | Order bounded context scaffold. |
| `inventory-service` | 8085 | Inventory bounded context scaffold. |
| `notification-service` | 8086 | Notification bounded context scaffold. |

Each service is a Java 17 Spring Boot application with Maven wrapper support, Spotless formatting, Actuator, JPA, Flyway, Redis, Kafka, validation, Web MVC, PostgreSQL, and Testcontainers-based test dependencies. The auth service also includes Spring Security, OAuth2 resource server/client support, JWT infrastructure, password hashing, refresh tokens, and an outbox publisher.

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

Pull requests and pushes to `main` run the CI matrix across all services. Pushes to `main` also build and publish Docker images to GitHub Container Registry. A successful image publication triggers the dev deployment workflow, while production deployment is manual through the `Deploy Prod` workflow and the protected `prod` environment.
