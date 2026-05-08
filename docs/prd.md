# Product Requirements Document

## Product Summary

Market Basket Platform is a backend platform for a grocery or market commerce experience. It is designed as a set of independently deployable Spring Boot services with clear bounded contexts for identity, customers, catalog, subscriptions, orders, inventory, and notifications.

The first implementation focus is the authentication foundation: users can register, log in, receive JWT access tokens, rotate refresh tokens, log out, and authenticate downstream requests through published JWKS keys.

## Target Users

- Shoppers who need secure account access and a reliable ordering experience.
- Store operators who need accurate catalog, stock, order, and customer information.
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
- This repository does not currently include an API gateway.
- This repository does not currently define Kubernetes manifests, Helm charts, or Terraform.
- Most non-auth services are currently scaffolds and should not be treated as feature-complete domain implementations.

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
- Product catalog CRUD and search.
- Subscription plan and renewal workflows.
- Basket, checkout, and order lifecycle.
- Inventory reservation and stock adjustment.
- Notification delivery through email, SMS, or push providers.
- API gateway, service-to-service authorization, and centralized request tracing.
- Database migrations with Flyway or Liquibase.
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
| Platform | Build and test every service on PRs to `main`. | Implemented through GitHub Actions. |
| Platform | Publish service images on `main`. | Implemented through GitHub Actions. |
| Platform | Deploy dev after successful image publishing. | Implemented through GitHub Actions. |
| Platform | Deploy prod manually. | Implemented through GitHub Actions. |

## Non-Functional Requirements

- Security: passwords must never be persisted or logged in plaintext.
- Security: refresh tokens must be stored only as hashes.
- Security: access tokens should remain short lived. The default is 15 minutes.
- Reliability: CI must build services independently so one service failure is visible without blocking diagnostics for others.
- Observability: services expose Actuator health and Prometheus metrics.
- Operability: deployments should be repeatable with `docker compose pull` and `docker compose up -d --remove-orphans`.
- Testability: services include Testcontainers dependencies for PostgreSQL and Kafka integration testing.

## Success Metrics

- PR CI is green before merge.
- All service images publish successfully from `main`.
- Dev deployment completes automatically after image publication.
- Auth registration and login flows pass automated tests.
- Health endpoints stay available after deployment.

## Open Questions

- Which API gateway or ingress layer will front the services?
- Which frontend or mobile clients will consume the auth flows?
- Which event schema registry or contract testing approach should govern Kafka topics?
- Which migration tool should own production database schema changes?
- What are the exact catalog, inventory, subscription, order, and notification domain workflows?
