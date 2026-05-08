# Documentation

This directory is the source of truth for product, architecture, delivery, and operations documentation for Market Basket Platform.

## Start Here

- [Product requirements](prd.md): product goals, users, scope, non-goals, and release criteria.
- [Architecture](architecture.md): service boundaries, infrastructure, data ownership, and security posture.
- [Diagrams](diagrams.md): system context, containers, auth flow, CI/CD, and deployment diagrams.
- [API reference](api.md): current HTTP endpoints and examples.
- [Local development](local-development.md): how to run, test, format, and troubleshoot locally.
- [CI/CD flow](cicd.md): GitHub Actions jobs, environments, secrets, images, and rollback notes.
- [Operations](operations.md): runtime configuration, observability, runbooks, and production concerns.
- [Auth service TDD plan](auth-service-tdd-plan.md): incremental test-driven plan for the auth bounded context.

## Repository Map

```text
market-basket-platform
  .github/workflows     GitHub Actions CI, image publishing, and deployment
  docs                  Product, architecture, delivery, and operations docs
  infra/postgres        Local database bootstrap SQL
  services              Spring Boot services
    auth-service
    catalog-service
    customer-service
    inventory-service
    notification-service
    order-service
    subscription-service
  docker-compose.yml    Local and simple server orchestration
```

## Documentation Maintenance Rules

- Update [architecture.md](architecture.md) when adding a service, database, broker, cache, or cross-service integration.
- Update [api.md](api.md) when adding, removing, or changing an HTTP endpoint.
- Update [cicd.md](cicd.md) when a workflow, environment, image tag, secret, or release rule changes.
- Update [operations.md](operations.md) when a new runtime variable, monitoring surface, or runbook action is introduced.
- Keep diagrams as Mermaid text so they remain reviewable in pull requests.
