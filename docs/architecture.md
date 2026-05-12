# Architecture

## Overview

Market Basket Platform is organized as a microservice backend. Each service is independently buildable, containerized, and configured through environment variables. Docker Compose provides a local and simple server runtime with PostgreSQL, MongoDB, Redis, Kafka, Kong Gateway, Prometheus, Alertmanager, Grafana, and SonarQube.

The current implemented domain depth is concentrated in `auth-service`, with seller store/membership/review foundations in `seller-service`, seller-owned category/product foundations in `catalog-service`, and stock/reservation foundations in `inventory-service`. Customer, subscription, order, and notification are Spring Boot bounded-context scaffolds with shared platform dependencies, migrations, contract-test footholds, and deployment wiring.

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
| Observability | Spring Actuator, Prometheus, Alertmanager, Grafana |
| Code quality | Spotless, local SonarQube configuration |

## Service Boundaries

| Service | Database | Responsibility |
| --- | --- | --- |
| `auth-service` | `market_auth` | Identity, credentials, access tokens, refresh tokens, OAuth2 login, JWKS, auth events. |
| `customer-service` | `market_customer` | Customer profile and customer account domain. |
| `seller-service` | `market_seller` | Seller store profile, platform approval workflow, staff membership, and seller event contracts. |
| `catalog-service` | `market_catalog` | Seller-owned categories, products, draft/published lifecycle, and catalog event contracts. |
| `subscription-service` | `market_subscription` | Subscription plans and recurring customer relationships. |
| `order-service` | `market_order` | Order placement and lifecycle. |
| `inventory-service` | `market_inventory` | Seller stock, availability reservations, and inventory event contracts. |
| `notification-service` | `market_notification` | Notification orchestration and delivery records. |

## Shared Infrastructure

- PostgreSQL 16 hosts one database per service in local Compose.
- Redis 7 is available to every service.
- Kafka 7.6.1 is available to every service on `kafka:29092` inside Compose and `localhost:9092` on the host.
- Kong Gateway fronts public HTTP APIs on `localhost:8000` with declarative DB-less configuration from `infra/kong/kong.yml`.
- MongoDB 7 is provisioned, but no current service configuration in this repo consumes it.
- Prometheus, Alertmanager, and Grafana containers are provisioned for monitoring. Prometheus scrapes Spring Actuator metrics, Kong status metrics, and PostgreSQL, Redis, and Kafka exporters.
- SonarQube Community Edition is available locally on `localhost:9000`; the committed Sonar properties currently target auth-service source and test coverage.

## Auth Service Internal Architecture

`auth-service` follows a ports-and-adapters style:

- `domain`: pure domain models such as `User`, `Email`, `Password`, `RefreshToken`, and `TokenFamily`.
- `application`: use cases and ports for persistence, token issuing, hashing, and event storage.
- `infrastructure`: Spring MVC controllers, Spring Security, JWT, crypto, JPA repositories, Kafka outbox publishing, and configuration.

Core auth use cases:

- `RegisterUserUseCase`
- `LoginWithPasswordUseCase`
- `RefreshTokenUseCase`
- `LogoutUseCase`
- `GoogleLoginUseCase`
- `AdminUserManagementUseCase`

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
- Kong currently routes `/auth/**` and `/.well-known/jwks.json` to auth-service. The Spring OAuth2 entry points exist on auth-service and still need Kong routes before gateway-only OAuth login is expected to work.
- Seller-service, catalog-service, and inventory-service validate auth-service JWTs through JWKS, issuer, and audience checks, then map `roles` to `ROLE_*` authorities and `permissions` to direct authorities. Public catalog reads remain open. Seller-service enforces ownership against its membership table. Catalog and inventory write paths require platform/service roles or active `seller_memberships` JWT claims for the target seller.

## Ownership Authorization

Seller ownership authorization is enforced at the service boundary after JWT validation:

- Seller-service is the source of truth for seller memberships. Store reads require an active seller membership or platform admin role, while membership management requires an active seller `OWNER` membership or platform admin role.
- Seller store creation and platform review operations use the JWT subject as the actor user id. Request-body actor ids are ignored when present for backward compatibility.
- Catalog-service keeps public read endpoints open, but product writes require `ADMIN`/`SUPER_ADMIN` or an active `seller_memberships` JWT claim matching the product seller.
- Inventory-service requires `ADMIN`, `SUPER_ADMIN`, `SERVICE`, or an active `seller_memberships` JWT claim matching the stock seller for stock and reservation access.
- Downstream `seller_memberships` claims are a compatibility contract with auth-issued tokens; seller-service remains the authoritative membership store.

## Eventing

The auth service has an outbox persistence model and Kafka publisher. This allows domain events to be stored transactionally with state changes and published after commit. Current implemented auth event names include:

- `auth.user.registered.v1`
- `auth.session.login_succeeded.v1`
- `auth.session.login_failed.v1`
- `auth.session.refresh_token_rotated.v1`
- `auth.session.refresh_token_reused.v1`
- `auth.session.revoked.v1`
- `auth.user.role_assigned.v1`
- `auth.user.role_removed.v1`
- `auth.user.account_suspended.v1`
- `auth.user.account_reactivated.v1`
- `auth.user.google_account_linked.v1`

Event contracts should remain versioned. Consumers should be tolerant of additive fields.
The first implemented event-governance mechanism is JSON Schema producer and consumer contract testing. `OutboxEvent` carries structured payload fields, the JPA adapter serializes payload JSON for the outbox table, and the auth outbox publisher serializes Kafka envelopes with Jackson. Seller-service has a producer contract for `seller.approved.v1`, catalog-service has a producer contract for `catalog.product.published.v1`, and inventory-service has producer contracts for `inventory.stock_reserved.v1` and `inventory.reservation_released.v1`. Catalog, subscription, order, and notification include first consumer contract tests using recorded examples. Seller/catalog/inventory outbox persistence and Kafka publishing remain deferred until consumers need those events at runtime. Schema Registry remains deferred until shared consumer pressure justifies the extra infrastructure.

## Data Ownership

Each service owns its database and should be the only writer to its own schema. Cross-service data sharing should happen through APIs or events, not direct database joins.

Local Compose creates all service databases from `infra/postgres/init.sql`.
Service-local Flyway migrations stored in each service's `src/main/resources/db/migration` directory now own schema creation and evolution. Auth, seller, catalog, and inventory have real domain tables; customer, subscription, order, and notification currently have placeholder `select 1` migrations. Hibernate validates mapped schemas instead of creating or updating production tables.
CI validates every service migration set against a disposable PostgreSQL database through the Flyway Maven plugin. Deployments run pinned Flyway Compose runners before app rollout, and services still run startup migrations as a safety net.

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
- Startup Flyway remains enabled as a safety net while deployment-time migration execution matures.
- No distributed tracing is configured yet.
- Resource ownership enforcement across services is still minimal; current downstream authorization gates by JWT role before deeper seller-membership lookups are introduced.
