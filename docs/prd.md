# Product Requirements Document

## Product Summary

Market Basket Platform is a backend platform for a grocery subscription marketplace. It is designed as a set of independently deployable Spring Boot services with clear bounded contexts for identity, customers, sellers, catalog, subscriptions, orders, inventory, and notifications.

The implemented foundation now covers authentication plus the first seller, catalog, and inventory slices: users can register, log in, receive JWT access tokens, rotate refresh tokens, log out, and authenticate downstream requests through published JWKS keys; sellers can be created and reviewed with membership-scoped access; products can be managed and published with seller ownership checks; and inventory can be stocked and reserved with seller ownership checks.

## Target Users

- Shoppers who need secure account access and subscribe to recurring grocery baskets of fruits, vegetables, and other fresh market products.
- Sellers who manage stores, catalog items, stock availability, prices, fulfillment windows, and order handoff.
- Platform operators who review sellers, manage disputes, monitor service health, and support customers.
- Backend engineers who need independently testable and deployable service boundaries.
- Operators who need reproducible deployments, health checks, and observable services.

## Goals

- Provide a secure identity foundation for the rest of the platform.
- Separate core commerce capabilities into service-owned bounded contexts.
- Use asynchronous messaging for domain events and future workflow integration.
- Keep local development reproducible with Docker Compose and per-service Maven wrappers.
- Provide a CI/CD path from pull request checks to image publication and environment deployment.

## Non-Goals

- This repository does not currently include a frontend application.
- This repository includes a local Kong Gateway configuration for public HTTP API routing.
- This repository does not currently define Kubernetes manifests, Helm charts, or Terraform.
- Seller, catalog, and inventory have first-pass domain APIs, but are not feature-complete marketplace implementations. Customer, subscription, order, and notification are still mostly scaffolds.

## MVP Scope

### Identity and Access

- Email/password registration.
- Email/password login.
- JWT access-token issuance.
- Refresh-token rotation with token-family revocation on reuse.
- Logout for one session.
- Logout for all user sessions.
- JWKS endpoint for downstream JWT validation.
- Google OAuth2 success handling entry point.

### Platform Foundation

- Per-service PostgreSQL databases.
- Kafka broker for event publication.
- Redis dependency provisioned for caching, rate limiting, or session-related capabilities.
- Actuator health, metrics, and Prometheus exposure.
- Docker images for each service.
- CI checks for formatting, tests, and packaging.
- Dev and prod deployment workflows.

## Future Scope

- Customer profile management.
- Product catalog search and read-model optimization.
- Seller approval enforcement during catalog publishing.
- Subscription plan and renewal workflows.
- Basket, checkout, and order lifecycle.
- Inventory reservation expiry, commit, decrement, and adjustment workflows.
- Notification delivery through email, SMS, or push providers.
- API gateway, service-to-service authorization, and centralized request tracing.
- Controlled pre-deployment database migration execution.
- Production-grade dashboards and alerts.

## Functional Requirements

| Area | Requirement | Current status |
| --- | --- | --- |
| Auth | Register user with normalized email and hashed password. | Implemented in `auth-service`. |
| Auth | Reject duplicate emails. | Implemented in `auth-service`. |
| Auth | Login with email and password. | Implemented in `auth-service`. |
| Auth | Issue JWT access tokens with issuer, audience, subject, email, timestamps, and key id. | Implemented in `auth-service`. |
| Auth | Store refresh tokens as hashes and rotate on refresh. | Implemented in `auth-service`. |
| Auth | Revoke refresh-token family on detected reuse. | Implemented in `auth-service`. |
| Auth | Publish auth domain events through an outbox. | Implemented in `auth-service`. |
| Auth | Expose marketplace roles and permissions in auth responses and JWT claims. | Implemented in `auth-service`. |
| Auth | Allow authorized admins to assign and revoke roles, suspend users, and reactivate users. | Implemented in `auth-service`. |
| Seller | Create seller stores and manage owner/staff memberships. | Implemented in `seller-service`. |
| Seller | Approve or reject seller stores through platform review. | Implemented in `seller-service`. |
| Catalog | Create categories and seller-owned products with draft, published, and unpublished lifecycle states. | Implemented in `catalog-service`. |
| Catalog | Define the first catalog product-published event contract. | Implemented as JSON Schema producer contract tests in `catalog-service`. |
| Inventory | Manage seller stock and reservation availability groundwork. | Implemented in `inventory-service`. |
| Inventory | Define stock reservation and release event contracts. | Implemented as JSON Schema producer contract tests in `inventory-service`. |
| Platform | Build and test every service on PRs to `main`. | Implemented through GitHub Actions. |
| Platform | Validate every service Flyway migration set against PostgreSQL on PRs to `main`. | Implemented through GitHub Actions. |
| Platform | Publish service images on `main`. | Implemented through GitHub Actions. |
| Platform | Deploy dev after successful image publishing. | Implemented through GitHub Actions. |
| Platform | Deploy dev and prod with explicit image tags and smoke checks. | Implemented through GitHub Actions. |
| Platform | Validate auth-service JWTs in downstream business services. | Implemented first in seller, catalog, and inventory services with role gates and issuer/audience validation. |
| Platform | Enforce seller ownership for protected seller, catalog, and inventory operations. | Implemented in seller, catalog, and inventory services. |
| Platform | Expose local observability and quality tools. | Implemented with Prometheus, Alertmanager, Grafana dashboards, exporters, Sentry wiring, and local SonarQube. |

## Non-Functional Requirements

- Security: passwords must never be persisted or logged in plaintext.
- Security: refresh tokens must be stored only as hashes.
- Security: access tokens should remain short lived. The default is 15 minutes.
- Reliability: CI must build services independently so one service failure is visible without blocking diagnostics for others.
- Observability: services expose Actuator health and Prometheus metrics.
- Operability: deployments should be repeatable with `docker compose pull` and `docker compose up -d --remove-orphans`.
- Testability: services include Testcontainers dependencies for PostgreSQL and Kafka integration testing.
- Event contracts: Kafka producers and consumers must publish or consume versioned event types and validate payload compatibility before merge.
- Database safety: production schema changes must be versioned, reviewed, repeatable, and executed by the owning service deployment.

## Success Metrics

- PR CI is green before merge.
- All service images publish successfully from `main`.
- Dev deployment completes automatically after image publication.
- Auth registration and login flows pass automated tests.
- Health endpoints stay available after deployment.

## Decisions and Implementation Plans

### API Gateway and Ingress

Decision: use Kong Gateway as the open-source API gateway in front of all public HTTP APIs.

Rationale:

- Kong has a mature open-source gateway, Docker support, JWT/OIDC-related plugins, rate limiting, request logging, Prometheus metrics, and a clear path from local Compose to Kubernetes ingress later.
- The platform already exposes multiple service ports locally. Kong should become the single public entry point while internal service ports remain private in production.
- Gateway configuration can be managed declaratively with Kong DB-less mode for local/dev, then promoted to Kong Ingress Controller or decK-managed configuration for Kubernetes.

Initial route plan:

| Public route | Service | Notes |
| --- | --- | --- |
| `/auth/**` | `auth-service` | Registration, login, refresh, logout, current-user, and admin user-management endpoints. |
| `/.well-known/jwks.json` | `auth-service` | Public JWKS route for downstream JWT validation. |
| `/customers/**` | `customer-service` | Shopper profile, addresses, preferences, payment profile references. |
| `/sellers/**` | `seller-service` | Seller onboarding, store profile, staff membership, compliance status, and store operations. |
| `/catalog/**` | `catalog-service` | Public browsing plus seller-owned product management. |
| `/subscriptions/**` | `subscription-service` | Shopper basket plans, recurrence rules, skips, pauses, renewals. |
| `/orders/**` | `order-service` | Checkout, order status, fulfillment tracking, cancellations. |
| `/inventory/**` | `inventory-service` | Seller/internal stock management and reservation APIs. |
| `/notifications/**` | `notification-service` | Internal/admin notification templates and preferences. |

Implementation status and plan:

1. Added a `kong` service to `docker-compose.yml` and exposed it on local port `8000`.
2. Added `infra/kong/kong.yml` with DB-less declarative services, routes, correlation IDs, and Prometheus plugin configuration.
3. Keep JWT validation in downstream services first while Kong handles routing, correlation IDs, and observability.
4. Use auth-service JWKS as the source of token validation when Kong-side JWT/OIDC validation is finalized.
5. Deployment smoke checks call the public JWKS route through Kong and verify Prometheus and Alertmanager readiness.
6. Configure public routes to require JWT except explicitly public auth endpoints and public catalog browsing once the Kong JWT/OIDC approach is chosen.
7. Add Kong routes for `/oauth2/**` and `/login/oauth2/**` before requiring Google OAuth2 to work through the gateway; those endpoints currently exist at the auth-service edge.
8. Keep direct service ports for local debugging only; production compose or Kubernetes exposure should publish only Kong and observability/admin endpoints intentionally.

### Client Applications

Decision: design the backend for three client surfaces, even though this repository does not implement them yet.

| Client | Primary users | Auth flow |
| --- | --- | --- |
| Shopper mobile app | Grocery subscribers | Email/password, refresh-token rotation, future Google OAuth2. |
| Seller web portal | Store owners and staff | Email/password, MFA later, seller-scoped permissions. |
| Platform admin console | Operations and support team | Admin login, stricter RBAC, audit logging, MFA before production. |

Near-term backend requirements:

- Auth responses should expose roles, account profile, and permissions clearly enough for clients to render role-specific navigation.
- Refresh-token rotation should support mobile and browser sessions independently.
- Logout-all must invalidate every refresh-token family for a user.
- Admin/seller actions must emit audit-friendly domain events.

### RBAC and Account Profiles

Decision: extend RBAC from a simple `CUSTOMER`/`ADMIN` model into marketplace-aware roles while preserving `CUSTOMER` as the default public registration role.

Recommended roles:

| Role | Purpose |
| --- | --- |
| `CUSTOMER` | Shopper who manages their own profile, subscriptions, baskets, and orders. |
| `SELLER_OWNER` | Owns one or more seller stores and can manage store profile, staff, catalog, stock, pricing, and fulfillment settings. |
| `SELLER_STAFF` | Operates catalog, stock, and order fulfillment for assigned seller stores with narrower permissions. |
| `SUPPORT_AGENT` | Platform support role for read-heavy customer/order assistance and limited operational actions. |
| `ADMIN` | Platform administrator for marketplace operations, seller review, and broad management actions. |
| `SUPER_ADMIN` | Break-glass/platform owner role that can grant or revoke administrative roles and change security-sensitive configuration. |

Recommended account profiles:

| Account profile | Meaning |
| --- | --- |
| `CUSTOMER` | Individual shopper account. |
| `SELLER` | Seller/store account linked to one or more users. |
| `PLATFORM` | Internal platform staff account. |

RBAC implementation status and remaining plan:

1. Added `SELLER_OWNER`, `SELLER_STAFF`, `SUPPORT_AGENT`, and `SUPER_ADMIN` to the auth domain role model.
2. Added permission-level checks for admin role assignment, role revocation, user suspension, and user reactivation.
3. Added permissions such as `SELLER_CATALOG_MANAGE`, `SELLER_INVENTORY_MANAGE`, `SELLER_ORDER_FULFILL`, `CUSTOMER_SUBSCRIPTION_MANAGE_OWN`, `PLATFORM_SELLER_REVIEW`, `AUTH_USER_ROLE_ASSIGN`, and `AUTH_USER_ROLE_REVOKE`.
4. Added role-change outbox events with actor, target user, and changed role.
5. Added seller membership records outside auth in `seller-service`.
6. Seller, catalog, and inventory writes now combine JWT roles with seller ownership checks. Seller-service checks its membership table; catalog and inventory require active `seller_memberships` JWT claims for seller-scoped writes.
7. Keep first-admin bootstrap operational and auditable; only `SUPER_ADMIN` should grant `ADMIN` or `SUPER_ADMIN` after bootstrap.

### Kafka Event Contracts and Testing

Decision: start event governance with JSON Schema plus contract tests.

Current recommendation: keep JSON Schema and consumer-driven contract tests as the first line of defense, then add a schema registry when event volume and cross-team coordination justify the operational cost.

Specialist recommendation for this repository: choose JSON Schema plus contract tests first. The current services publish JSON strings through `KafkaTemplate<String, String>`, Docker Compose runs Kafka without Schema Registry, and there are not yet active cross-service Kafka consumers. Avro or Protobuf with Schema Registry should remain later options when event contracts become shared by multiple production consumers.

Implementation status:

- Auth events have JSON Schema producer contract tests in `auth-service`, including registration, login, refresh-token, session revocation, role-change, account-state, and Google account-link events.
- `OutboxEvent` now carries structured payload fields, and the JPA outbox adapter serializes payloads to JSON for persistence.
- `KafkaOutboxPublisher` now builds the event envelope with Jackson instead of string formatting.
- A Testcontainers Kafka integration test proves pending outbox events are published with the expected topic, key, envelope, payload object, and published status.
- Seller, catalog, and inventory have JSON Schema producer contract tests plus recorded example payloads.
- Catalog, subscription, order, and notification include first consumer contract tests around seller-approved, product-published, inventory, and notification-relevant events.
- Kafka, PostgreSQL, and Redis Testcontainers images are pinned in auth-service tests.
- Schema Registry is intentionally deferred until multiple production consumers depend on shared topics or manual compatibility review becomes too expensive.

Option comparison:

| Option | Fit | Concerns |
| --- | --- | --- |
| JSON Schema plus contract tests | Best first step because it matches the current JSON outbox implementation and adds low infrastructure overhead. | Compatibility discipline lives in tests and review conventions, not broker-enforced registry policy. |
| Avro plus Schema Registry | Good later for high-volume, strongly governed shared events. | Adds Schema Registry, serializers, code generation, compatibility policy, and more local/CI/prod infrastructure. |
| Protobuf plus Schema Registry | Good later for polyglot consumers, strict typed contracts, or alignment with gRPC-style APIs. | More migration friction from the current JSON model and extra care around defaults and optional fields. |

Specialist review brief:

- Analyze the existing Spring Boot services, outbox event implementation, Docker Compose Kafka setup, and Testcontainers dependencies.
- Compare three options: JSON Schema plus contract tests, Avro plus Schema Registry, and Protobuf plus Schema Registry.
- Identify risks around local development complexity, CI time, backward compatibility, event versioning, developer ergonomics, and production operability.
- Recommend a first milestone that catches breaking event changes before merge without overbuilding the platform.

Repo-specific specialist concerns:

- `KafkaOutboxPublisher` currently sends each event to a topic named exactly like the event type, for example `auth.user.registered.v1`. Decide before production whether to keep one-topic-per-event-type or move to domain topics such as `auth.events` with `eventType` inside the envelope.
- Existing contract tests prove producer schemas, selected consumer examples, and one auth broker publish path. They do not yet prove runtime consumers or consumer idempotency.

Concerns the specialist should explicitly evaluate:

| Concern | Why it matters |
| --- | --- |
| Backward compatibility | Consumers must survive producer deployments that add optional fields or introduce new event versions. |
| Runtime dependencies | Schema Registry adds infrastructure that must run locally, in CI, and in production. |
| Contract ownership | Each topic needs a clear owner and review process before other services depend on it. |
| Event naming | Event types should stay explicit and versioned, for example `auth.user.registered.v1`. |
| Payload validation | Tests should validate required fields, enum values, timestamps, IDs, and correlation metadata. |
| Outbox reliability | Publishing tests must prove database writes and event writes stay atomic from the service point of view. |
| Consumer idempotency | Consumers must handle duplicate events and retries safely. |
| Observability | Events should carry correlation IDs and enough metadata for tracing across services. |

Remaining event-contract plan:

1. Continue adding recorded example event documents beside schemas for consumer teams.
2. Decide the topic strategy: event-type topics now versus domain topics such as `auth.events`.
3. Expand consumer contract tests in each consuming service using recorded example events.
4. Require compatibility checks in CI: additive optional fields are allowed in the same version; removed fields, renamed fields, type changes, and semantic changes require a new event version.
5. Revisit Schema Registry when two or more services actively consume the same topic in production or when event evolution becomes difficult to review manually.

### Production Database Migrations

Decision: use Flyway as the production migration tool for all PostgreSQL-backed Spring Boot services.

Rationale:

- Flyway is simple, widely used with Spring Boot, and fits service-owned relational schemas well.
- Versioned SQL migrations are easy to review in PRs and keep close to the service that owns the schema.
- The project is still early; Liquibase's richer change model is not needed yet.

Implementation status:

- Flyway dependencies and the Flyway Maven plugin are configured for every PostgreSQL-backed service.
- Migrations are stored under each service's `src/main/resources/db/migration` directory.
- Hibernate defaults to schema validation through `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway runs automatically when services start in local, CI, and deployed Compose environments.
- CI includes a dedicated PostgreSQL-backed migration validation matrix that applies migrations, validates them, and prints Flyway info for every service database.
- Dev and prod deployment workflows run controlled Flyway migration runners before application rollout.
- `auth-service` owns schema migrations for users, roles, OAuth accounts, refresh tokens, and outbox events.
- `seller-service` owns schema migrations for seller stores, owner/staff memberships, and approval review state.
- `catalog-service` owns schema migrations for categories and seller-owned products.
- `inventory-service` owns schema migrations for stock records and reservations.
- Customer, subscription, order, and notification currently have placeholder initial migrations so future domain migrations append cleanly without reintroducing Hibernate schema generation.

Remaining production migration work:

- Keep startup Flyway enabled as a safety net while the controlled deployment migration step matures.
- Keep rollback practice forward-only: add fix migrations rather than destructive down migrations.
- Require migration review for locking risk, data backfills, nullable-to-not-null transitions, indexes on large tables, and backward compatibility during rolling deploys.

### Next Implementation Path

The previous platform-hardening roadmap candidates now have initial implementation. Auth, event contracts, Flyway migrations, deployment migration gates, seller membership, catalog foundation, immutable image deployment, and smoke checks are in place. The product roadmap should continue shifting from platform foundation toward marketplace domain depth.

1. Catalog Foundation
   - Implemented: product and category CRUD foundation in `catalog-service`.
   - Implemented: seller-owned products with draft, published, and unpublished lifecycle states.
   - Implemented: first catalog event contract, `catalog.product.published.v1`.
2. Seller Approval
   - Implemented: seller stores now start in `PENDING_REVIEW` and can be approved or rejected by platform review endpoints.
   - Implemented: first seller event contract, `seller.approved.v1`.
   - Remaining: enforce seller approval during catalog publishing once inter-service authorization and seller lookup are added.
3. Inventory Foundation
   - Implemented: seller-managed stock records tied to catalog product ids.
   - Implemented: availability and reservation groundwork for order and subscription workflows.
   - Implemented: first inventory event contracts, `inventory.stock_reserved.v1` and `inventory.reservation_released.v1`.
   - Remaining: reservation commit/expiry and order-driven stock decrement behavior.

Consumer-side event contracts, local observability, and downstream JWT plus ownership authorization now have initial implementation. Remaining hardening tracks include deeper consumer adoption, production incident routing, rate limiting, and distributed tracing.

### Marketplace Domain Workflows

Decision: model the product as a grocery subscription marketplace where shoppers subscribe to recurring produce baskets and sellers fulfill orders for fresh products.

Catalog workflow:

1. Seller creates a store profile and submits it for platform review.
2. Platform approves the seller and enables catalog publishing.
3. Seller creates products with category, unit, package size, price, photos, seasonal availability, and quality notes.
4. Seller groups products into basket-eligible items, optional add-ons, and one-time purchase items.
5. Catalog publishes product and price changes as events for subscription, order, and search/read-model consumers.

Inventory workflow:

1. Seller records available stock by product, harvest/arrival date, quality grade, and fulfillment window.
2. Inventory service exposes availability to catalog and subscription planning.
3. Checkout or subscription renewal reserves stock for a short window.
4. Order confirmation commits the reservation.
5. Cancellation, payment failure, or reservation expiry releases stock.
6. Fulfillment completion decrements final stock and records shrinkage or substitutions when needed.

Subscription workflow:

1. Shopper chooses a basket plan, delivery/pickup cadence, seller or marketplace-curated basket, address, preferences, allergies/dislikes, and substitution rules.
2. Subscription service schedules renewal windows and emits upcoming-renewal events.
3. Before each renewal, the shopper can skip, pause, change basket size, add one-time items, or edit preferences.
4. Renewal creates a draft order from plan rules, available catalog, and inventory constraints.
5. Payment authorization converts the draft into a confirmed order.
6. Failed payment pauses or retries the renewal according to policy.

Order workflow:

1. Shopper checks out directly or receives a generated subscription order.
2. Order service prices items, delivery fees, discounts, and seller split details.
3. Order service requests inventory reservation.
4. Payment authorization confirms the order.
5. Seller accepts, packs, substitutes if allowed, and marks ready for pickup or delivery.
6. Shopper receives status updates.
7. Completion closes the order, emits settlement/analytics events, and optionally requests feedback.
8. Cancellation rules depend on order state, fulfillment window, and seller policy.

Notification workflow:

1. Services emit notification-intent events such as registration confirmation, upcoming renewal, payment failed, order confirmed, seller accepted order, substitution requested, ready for pickup, out for delivery, and delivered.
2. Notification service resolves user preferences, channel eligibility, templates, locale, and rate limits.
3. Notification service sends email first for MVP; SMS and push can be added later.
4. Delivery attempts, failures, provider IDs, and suppression decisions are stored for audit and support.

Cross-service events to define first:

| Event | Producer | Consumers |
| --- | --- | --- |
| `auth.user.registered.v1` | `auth-service` | `customer-service`, `notification-service` |
| `auth.user.role_assigned.v1` | `auth-service` | `customer-service`, audit/read models |
| `seller.approved.v1` | `seller-service` | `catalog-service`, `notification-service` |
| `catalog.product.published.v1` | `catalog-service` | `subscription-service`, `order-service`, search/read models |
| `inventory.stock_reserved.v1` | `inventory-service` | `order-service` |
| `inventory.reservation_released.v1` | `inventory-service` | `order-service` |
| `subscription.renewal_due.v1` | `subscription-service` | `order-service`, `notification-service` |
| `order.confirmed.v1` | `order-service` | `inventory-service`, `notification-service`, seller read models |
| `order.fulfillment_status_changed.v1` | `order-service` | `notification-service`, customer/seller read models |
