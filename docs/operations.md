# Operations

## Runtime Configuration

Services are configured primarily through environment variables. Docker Compose sets service ports, datasource URLs, Kafka bootstrap servers, Redis host/port, and auth token properties.

Common service dependencies:

- PostgreSQL: service-owned databases under one local PostgreSQL instance.
- Redis: available to every service.
- Kafka: available to every service.
- Prometheus and Grafana: provisioned for observability.

## Health Checks

Auth service health:

```http
GET http://localhost:8080/actuator/health
```

Other services follow the same Actuator path on their ports:

- Customer: `8081`
- Catalog: `8082`
- Subscription: `8083`
- Order: `8084`
- Inventory: `8085`
- Notification: `8086`

## Metrics

Application configuration exposes:

```text
health,info,metrics,prometheus
```

Prometheus is available on `localhost:9090`, but scrape configuration is not yet committed. Add a Prometheus configuration before relying on dashboards or alerts.

## Logs

Local Compose logs:

```bash
docker compose logs -f auth-service
```

Deployment host logs use the same Compose command from the deployed repository path.

## Runbook: Service Fails To Start

1. Check container state with `docker compose ps`.
2. Inspect logs with `docker compose logs -f <service>`.
3. Confirm PostgreSQL, Redis, and Kafka are healthy.
4. Confirm required environment variables are present on the host.
5. Confirm the image tag exists in GitHub Container Registry.
6. Restart the service with `docker compose up -d <service>`.

## Runbook: Database Connection Failure

1. Confirm PostgreSQL is running.
2. Confirm the service datasource URL points at the Compose hostname `postgres` inside Docker.
3. Confirm the target database exists. Local databases are created by `infra/postgres/init.sql`.
4. Confirm `POSTGRES_USER` and `POSTGRES_PASSWORD` match service environment variables.

## Runbook: Kafka Connection Failure

1. Confirm Zookeeper and Kafka are running.
2. Inside Docker Compose, services should use `kafka:29092`.
3. From the host, local tools should use `localhost:9092`.
4. Check Kafka health with `docker compose ps kafka`.

## Security Operations

- Treat JWT signing configuration as sensitive production configuration.
- Rotate JWT keys intentionally and keep old public keys available long enough for existing access tokens to expire.
- Keep refresh tokens opaque and hashed at rest.
- Keep access-token TTL short. The default is 15 minutes.
- Do not log passwords, refresh tokens, access tokens, or OAuth provider credentials.
- Use environment-specific OAuth2 client credentials outside source control.

## Backup And Recovery

Production should define backups for PostgreSQL data volumes before handling real user data. At minimum:

- Schedule database backups.
- Test restore procedures.
- Document retention windows.
- Keep deployment artifacts tied to Git SHAs.

## Production Readiness Checklist

- Replace `ddl-auto=update` with a migration tool such as Flyway or Liquibase.
- Define Prometheus scrape configuration and alert rules.
- Add dashboard definitions for service latency, error rate, JVM, database, Kafka, and Redis health.
- Pin production Compose deployments to immutable image tags.
- Add HTTPS and an ingress or API gateway.
- Add centralized logs.
- Add secret management outside plain environment files.
- Add rate limiting for auth endpoints.
- Add smoke tests after deployment.
