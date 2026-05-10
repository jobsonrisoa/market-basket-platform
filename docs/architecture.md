# Architecture

## Overview

Market Basket Platform is organized as a microservice backend. Each service is independently buildable, containerized, and configured through environment variables. Docker Compose provides a local and simple server runtime with PostgreSQL, MongoDB, Redis, Kafka, Kong Gateway, Prometheus, and Grafana.

The current implemented domain depth is concentrated in `auth-service`. The remaining services are Spring Boot bounded-context scaffolds with shared platform dependencies and deployment wiring.

## Technology Stack

| Layer | Technology |
| --- | --- |
| Runtime | Java 17 |
| Application framework | Spring Boot |
| HTTP | Spring Web MVC |
| Persistence | Spring Data JPA, PostgreSQL |
| Cache/session dependency | Redis |
| Messaging | Kafka with Zookeeper |
| API gateway | Kong Gateway in DB-less mode |
| Security | Spring Security, OAuth2 resource server/client, JWT |
| Tests | JUnit, Spring Boot Test, Testcontainers |
| Formatting | Spotless Maven plugin |
| Containers | Docker, Docker Compose |
| Registry | GitHub Container Registry |
| CI/CD | GitHub Actions |
| Observability | Spring Actuator, Prometheus, Grafana |

## Service Boundaries

| Service | Database | Responsibility |
| --- | --- | --- |
| `auth-service` | `market_auth` | Identity, credentials, access tokens, refresh tokens, OAuth2 login, JWKS, auth events. |
| `customer-service` | `market_customer` | Customer profile and customer account domain. |
| `seller-service` | `market_seller` | Seller onboarding, store profile, staff membership, and seller operations. |
| `catalog-service` | `market_catalog` | Product catalog domain. |
| `subscription-service` | `market_subscription` | Subscription plans and recurring customer relationships. |
| `order-service` | `market_order` | Order placement and lifecycle. |
| `inventory-service` | `market_inventory` | Stock, reservations, and inventory adjustments. |
| `notification-service` | `market_notification` | Notification orchestration and delivery records. |

## Shared Infrastructure

- PostgreSQL 16 hosts one database per service in local Compose.
- Redis 7 is available to every service.
- Kafka 7.6.1 is available to every service on `kafka:29092` inside Compose and `localhost:9092` on the host.
- Kong Gateway fronts public HTTP APIs on `localhost:8000` with declarative DB-less configuration from `infra/kong/kong.yml`.
- MongoDB 7 is provisioned, but no current service configuration in this repo consumes it.
- Prometheus and Grafana containers are provisioned for monitoring. Prometheus scrapes Spring Actuator metrics and Kong status metrics.

## Auth Service Internal Architecture

`auth-service` follows a ports-and-adapters style:

- `domain`: pure domain models such as `User`, `Email`, `Password`, `RefreshToken`, and `TokenFamily`.
- `application`: use cases and ports for persistence, token issuing, hashing, and event storage.
- `infrastructure`: Spring MVC controllers, Spring Security, JWT, crypto, JPA repositories, Kafka outbox publishing, and configuration.

Core use cases:

- `RegisterUserUseCase`
- `LoginWithPasswordUseCase`
- `RefreshTokenUseCase`
- `LogoutUseCase`
- `GoogleLoginUseCase`

Persistence entities include:

- `users`
- `password_credentials`
- `oauth_accounts`
- `refresh_tokens`
- `refresh_token_families`
- `outbox_events`

## Authentication Model

- Public endpoints: `/auth/register`, `/auth/login`, `/auth/refresh`, `/.well-known/jwks.json`, `/oauth2/**`, `/login/oauth2/**`, and `/actuator/health`.
- Protected endpoints require JWT authentication through Spring OAuth2 resource server support.
- Access tokens are JWTs.
- Refresh tokens are opaque secrets returned to clients and also set as an HTTP-only cookie by login and refresh responses.
- Refresh-token reuse revokes the whole token family.
- JWKS is exposed from `/.well-known/jwks.json` so downstream services can validate JWTs without calling the auth service for every request.

## Eventing

The auth service has an outbox persistence model and Kafka publisher. This allows domain events to be stored transactionally with state changes and published after commit. Current planned or implemented event names include:

- `auth.user.registered.v1`
- `auth.session.login_succeeded.v1`
- `auth.session.login_failed.v1`
- `auth.session.refresh_token_rotated.v1`
- `auth.session.refresh_token_reused.v1`

Event contracts should remain versioned. Consumers should be tolerant of additive fields.

## Data Ownership

Each service owns its database and should be the only writer to its own schema. Cross-service data sharing should happen through APIs or events, not direct database joins.

Local Compose creates all service databases from `infra/postgres/init.sql`.
Flyway migrations stored in each service's `src/main/resources/db/migration` directory own schema creation and evolution. Hibernate validates mapped schemas instead of creating or updating production tables.

## Deployment Architecture

Each service has its own Dockerfile and image:

- `market-auth-service`
- `market-customer-service`
- `market-seller-service`
- `market-catalog-service`
- `market-subscription-service`
- `market-order-service`
- `market-inventory-service`
- `market-notification-service`

Images are tagged with both the Git SHA and `main` on pushes to `main`. Deployment workflows connect to a host over SSH, update the repository, pull images, and restart the Compose stack.

## Known Architecture Gaps

- No centralized service discovery is defined.
- No dedicated pre-deployment migration job is configured yet; services currently run Flyway on startup.
- No distributed tracing is configured yet.
- No production Prometheus scrape configuration is committed yet.
- No explicit inter-service authorization model is configured outside auth JWT issuance.
