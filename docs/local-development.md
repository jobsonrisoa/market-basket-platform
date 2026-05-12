# Local Development

## Prerequisites

- JDK 17
- Docker and Docker Compose
- Git

Each service includes its own Maven wrapper, so a system Maven installation is optional.

## Run The Platform

From the repository root:

```bash
docker compose up --build
```

This starts:

- PostgreSQL on `localhost:5432`
- MongoDB on `localhost:27017`
- Redis on `localhost:6379`
- Zookeeper on `localhost:2181`
- Kafka on `localhost:9092`
- Kong Gateway on `localhost:8000`
- Kong Admin API on `localhost:8001`
- Services on ports `8080` through `8087`
- Prometheus on `localhost:9090`
- Grafana on `localhost:3000`
- Alertmanager on `localhost:9093`
- SonarQube on `localhost:9000`

Stop the platform:

```bash
docker compose down
```

## Run One Service Locally

Start dependencies:

```bash
docker compose up postgres redis kafka zookeeper
```

Run a service:

```bash
cd services/auth-service
./mvnw spring-boot:run
```

The default application configuration points to local PostgreSQL, Kafka, and Redis ports.

## Build And Test

Run checks for one service:

```bash
cd services/auth-service
./mvnw -B -ntp spotless:check package
```

Run tests only:

```bash
cd services/auth-service
./mvnw -B -ntp test
```

Apply formatting:

```bash
cd services/auth-service
./mvnw -B -ntp spotless:apply
```

Repeat per service as needed:

- `services/auth-service`
- `services/customer-service`
- `services/seller-service`
- `services/catalog-service`
- `services/subscription-service`
- `services/order-service`
- `services/inventory-service`
- `services/notification-service`

## Environment Variables

Common Compose variables:

Copy `.env.example` to `.env` for local Compose overrides. Keep `.env` local-only and do not commit environment-specific secrets.

| Variable | Default |
| --- | --- |
| `POSTGRES_USER` | `market` |
| `POSTGRES_PASSWORD` | `market` |
| `POSTGRES_DB` | `market` |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `REGISTRY` | `ghcr.io` |
| `IMAGE_NAMESPACE` | `jobson` |
| `IMAGE_TAG` | `local` |

Observability variables:

| Variable | Default |
| --- | --- |
| `SENTRY_DSN` | empty |
| `SENTRY_ENVIRONMENT` | `local` |
| `SENTRY_RELEASE` | `market-basket-platform@local` |
| `SENTRY_TRACES_SAMPLE_RATE` | `0.0` |

Auth variables:

| Variable | Default |
| --- | --- |
| `AUTH_JWT_ISSUER` | `http://localhost:8000` |
| `AUTH_JWT_AUDIENCE` | `market-basket-platform` |
| `AUTH_JWT_JWK_SET_URI` | `http://auth-service:8080/.well-known/jwks.json` in Compose |
| `AUTH_JWT_KEY_ID` | `local-dev-key` |
| `AUTH_JWT_ACCESS_TOKEN_TTL` | `15m` |
| `AUTH_REFRESH_TOKEN_TTL` | `30d` |

Customer-service event variables:

| Variable | Default |
| --- | --- |
| `AUTH_USER_REGISTERED_TOPIC` | `auth.user.registered.v1` |

## Database Bootstrap

`infra/postgres/init.sql` creates one database per service:

- `market_auth`
- `market_customer`
- `market_seller`
- `market_catalog`
- `market_subscription`
- `market_order`
- `market_inventory`
- `market_notification`

Flyway runs automatically when services start. Hibernate is configured for schema validation, so schema changes must be added as versioned SQL migrations under each service's `src/main/resources/db/migration` directory.

Validate one service's migrations against a running local PostgreSQL database:

```bash
cd services/auth-service
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/market_auth \
SPRING_DATASOURCE_USERNAME=market \
SPRING_DATASOURCE_PASSWORD=market \
./mvnw -B -ntp flyway:migrate flyway:validate flyway:info
```

Use a fresh local database for validation when possible. Running Flyway against an older database that was created before Flyway was introduced may fail because the schema has tables but no Flyway history table.

Run the same controlled migration step used by deployment:

```bash
./scripts/run-migrations.sh
```

## Local Quality Tools

SonarQube Community Edition is available when the full Compose stack is running:

```text
http://localhost:9000
```

The committed `sonar-project.properties` currently targets auth-service source, tests, bytecode, Surefire reports, and JaCoCo XML coverage. Broaden it when Sonar analysis is expanded to the other services.

## Troubleshooting

Check containers:

```bash
docker compose ps
```

Rebuild services:

```bash
docker compose build
```

Follow one service log:

```bash
docker compose logs -f auth-service
```

If PostgreSQL initialization changes, remove the named volume before restarting:

```bash
docker compose down -v
docker compose up --build
```
