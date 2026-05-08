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
- Services on ports `8080` through `8086`
- Prometheus on `localhost:9090`
- Grafana on `localhost:3000`

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
- `services/catalog-service`
- `services/subscription-service`
- `services/order-service`
- `services/inventory-service`
- `services/notification-service`

## Environment Variables

Common Compose variables:

| Variable | Default |
| --- | --- |
| `POSTGRES_USER` | `market` |
| `POSTGRES_PASSWORD` | `market` |
| `POSTGRES_DB` | `market` |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `REGISTRY` | `ghcr.io` |
| `IMAGE_NAMESPACE` | `jobson` |
| `IMAGE_TAG` | `local` |

Auth variables:

| Variable | Default |
| --- | --- |
| `AUTH_JWT_ISSUER` | `http://localhost:8080` |
| `AUTH_JWT_AUDIENCE` | `market-basket-platform` |
| `AUTH_JWT_KEY_ID` | `local-dev-key` |
| `AUTH_JWT_ACCESS_TOKEN_TTL` | `15m` |
| `AUTH_REFRESH_TOKEN_TTL` | `30d` |

## Database Bootstrap

`infra/postgres/init.sql` creates one database per service:

- `market_auth`
- `market_customer`
- `market_catalog`
- `market_subscription`
- `market_order`
- `market_inventory`
- `market_notification`

Spring JPA currently defaults to `ddl-auto=update`, so local schemas are updated by Hibernate when services start.

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
