# Operations

## Runtime Configuration

Services are configured primarily through environment variables. Docker Compose sets service ports, datasource URLs, Kafka bootstrap servers, Redis host/port, and auth token properties.

Common service dependencies:

- PostgreSQL: service-owned databases under one local PostgreSQL instance.
- Redis: available to every service.
- Kafka: available to every service.
- Kong Gateway: public local entry point for HTTP APIs.
- Prometheus: scrapes service Actuator metrics from `infra/prometheus/prometheus.yml`.
- Alertmanager: routes Prometheus alerts through `infra/alertmanager/alertmanager.yml`.
- Grafana: provisioned with a Prometheus datasource and market service/infrastructure dashboards.
- Sentry: optional error and tracing sink configured by environment variables.
- SonarQube: local code-quality service for manual analysis; not part of runtime serving.

Observability environment variables:

- `SENTRY_DSN`: enables Sentry when set. Leave empty for local development without Sentry.
- `SENTRY_ENVIRONMENT`: logical environment name, defaults to `local`.
- `SENTRY_RELEASE`: release identifier, defaults to `market-basket-platform@local` in Compose.
- `SENTRY_TRACES_SAMPLE_RATE`: distributed tracing sample rate, defaults to `0.0`.
- `GRAFANA_ADMIN_USER`: local Grafana admin user, defaults to `admin`.
- `GRAFANA_ADMIN_PASSWORD`: local Grafana admin password, defaults to `admin`.
- Alert routing: local Compose mounts `infra/alertmanager/alertmanager.yml` with a placeholder webhook URL. Production should mount an environment-specific Alertmanager config that points to Slack, PagerDuty, or another incident route without committing secrets.

## Health Checks

Auth service health:

```http
GET http://localhost:8080/actuator/health
```

Other services follow the same Actuator path on their ports:

- Customer: `8081`
- Seller: `8087`
- Catalog: `8082`
- Subscription: `8083`
- Order: `8084`
- Inventory: `8085`
- Notification: `8086`

Gateway checks:

```http
GET http://localhost:8000/.well-known/jwks.json
GET http://localhost:8100/status
```

## Metrics

Application configuration exposes:

```text
health,info,metrics,prometheus
```

Prometheus is available at:

```text
http://localhost:9090
```

Prometheus scrapes every service at `/actuator/prometheus` through Docker Compose DNS. The scrape targets are defined in `infra/prometheus/prometheus.yml`.
Kong metrics are exposed from the Kong status listener and scraped by Prometheus. PostgreSQL, Redis, and Kafka metrics are scraped through dedicated exporters in Docker Compose.

Useful local checks:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/health
curl http://localhost:8000/.well-known/jwks.json
curl http://localhost:8100/status
curl http://localhost:9090/-/ready
curl http://localhost:9090/api/v1/targets
curl http://localhost:9093/-/ready
curl http://localhost:9000/api/system/status
```

## Dashboards

Grafana is available at:

```text
http://localhost:3000
```

Default local credentials are `admin` / `admin` unless overridden. Grafana provisions:

- Datasource: `Prometheus`
- Dashboard folder: `Market Basket`
- Dashboard: `Market Services`
- Dashboard: `Market Infrastructure`

## Alerts

Prometheus alert rules live in `infra/prometheus/alerts.yml`.

Current local alerts:

- `MarketServiceDown`: a service cannot be scraped for at least 1 minute.
- `MarketServiceHighErrorRate`: more than 5% of HTTP responses are 5xx for 5 minutes.
- `MarketServiceHighLatencyP95`: HTTP p95 latency is above 1 second for 5 minutes.
- `MarketServiceHighHeapUsage`: JVM heap usage is above 85% for 10 minutes.
- `MarketPostgreSQLUnavailable`: PostgreSQL exporter or database is unavailable.
- `MarketRedisUnavailable`: Redis exporter or server is unavailable.
- `MarketKafkaUnavailable`: Kafka exporter or broker is unavailable.
- `MarketMigrationRunnerFailure`: migration runner visibility is missing after deployment.

Alertmanager is available at `http://localhost:9093`. The committed route points at a local placeholder webhook. Production environments should mount an environment-specific Alertmanager config with a real Slack, PagerDuty, incident-management, or webhook bridge endpoint.

## Error Tracking

Sentry is wired into every Spring service through `sentry-spring-boot-4-starter`. It is disabled by default because `SENTRY_DSN` is empty in local Compose.

To enable it locally, copy `.env.example` to `.env` and set local-only values. For shared environments, provide environment-specific overrides through the deployment host or secret manager rather than committing `.env`:

```bash
SENTRY_DSN=https://examplePublicKey@o0.ingest.sentry.io/0
SENTRY_ENVIRONMENT=staging
SENTRY_RELEASE=market-basket-platform@<git-sha>
SENTRY_TRACES_SAMPLE_RATE=0.1
```

Restart affected services after changing these values:

```bash
docker compose up -d --force-recreate auth-service customer-service seller-service catalog-service subscription-service order-service inventory-service notification-service
```

Keep `SENTRY_TRACES_SAMPLE_RATE` low in production until baseline traffic and cost are understood.

## Code Quality

Local Compose includes SonarQube Community Edition at `http://localhost:9000`. The checked-in `sonar-project.properties` currently focuses on auth-service analysis and coverage report paths. Treat SonarQube as a developer quality tool, not a production dependency.

## Logs

Local Compose logs:

```bash
docker compose logs -f auth-service
```

Deployment host logs use the same Compose command from the deployed repository path.

Centralized log aggregation is not configured yet. Prefer structured JSON logs before adding Loki, OpenSearch, or another log backend.

## Runbook: Service Fails To Start

1. Check container state with `docker compose ps`.
2. Inspect logs with `docker compose logs -f <service>`.
3. Confirm PostgreSQL, Redis, and Kafka are healthy.
4. Confirm required environment variables are present on the host.
5. Confirm the image tag exists in GitHub Container Registry.
6. Restart the service with `docker compose up -d <service>`.
7. Re-run deployment smoke checks for auth health, JWKS through Kong, Prometheus readiness, Prometheus targets, and Alertmanager readiness.

## Runbook: Database Connection Failure

1. Confirm PostgreSQL is running.
2. Confirm the service datasource URL points at the Compose hostname `postgres` inside Docker.
3. Confirm the target database exists. Local databases are created by `infra/postgres/init.sql`.
4. Confirm `POSTGRES_USER` and `POSTGRES_PASSWORD` match service environment variables.

## Runbook: Flyway Migration Failure

1. Inspect service logs for the failed migration version and SQL error.
2. Confirm the service is connecting to its own database, for example `market_auth` for auth-service.
3. Confirm CI migration validation passed for the same service and migration version.
4. Re-run the controlled migration step from the deployed repository path with `./scripts/run-migrations.sh`.
5. If the environment predates Flyway, confirm the controlled runner created a baseline history entry instead of attempting to recreate existing tables.
6. Do not edit an already-applied migration in a shared environment; add a new forward-only fix migration.
7. For local-only broken migration history, recreate the local PostgreSQL volume after confirming no useful local data needs to be kept.

## Runbook: Kafka Connection Failure

1. Confirm Zookeeper and Kafka are running.
2. Inside Docker Compose, services should use `kafka:29092`.
3. From the host, local tools should use `localhost:9092`.
4. Check Kafka health with `docker compose ps kafka`.
5. For customer profile creation from registrations, confirm `customer-service` can consume the `auth.user.registered.v1` topic or the configured `AUTH_USER_REGISTERED_TOPIC`.
6. For catalog publishing eligibility, confirm `seller-service` can publish and `catalog-service` can consume `seller.approved.v1`/`seller.rejected.v1` or the configured `SELLER_APPROVED_TOPIC`/`SELLER_REJECTED_TOPIC`.

## Runbook: Missing Prometheus Target

1. Open `http://localhost:9090/targets`.
2. Confirm the target hostname and port match `infra/prometheus/prometheus.yml`.
3. Confirm the service is running with `docker compose ps <service>`.
4. Confirm the metrics endpoint responds from the host, for example `curl http://localhost:8080/actuator/prometheus`.
5. Check service logs for Actuator, security, or startup errors.

## Runbook: Grafana Dashboard Has No Data

1. Confirm Prometheus is healthy at `http://localhost:9090/-/ready`.
2. Confirm targets are up at `http://localhost:9090/targets`.
3. In Grafana, confirm the `Prometheus` datasource points to `http://prometheus:9090`.
4. Confirm services were rebuilt after adding `micrometer-registry-prometheus`.

## Runbook: Alertmanager Not Routing Alerts

1. Confirm Alertmanager is healthy at `http://localhost:9093/-/ready`.
2. Confirm Prometheus has Alertmanager discovery at `http://localhost:9090/status`.
3. Confirm the deployed Alertmanager config mounts an environment-specific receiver URL.
4. Inspect `docker compose logs -f alertmanager` for notification delivery errors.

## Security Operations

- Treat JWT signing configuration as sensitive production configuration.
- Rotate JWT keys intentionally and keep old public keys available long enough for existing access tokens to expire.
- Keep refresh tokens opaque and hashed at rest.
- Keep access-token TTL short. The default is 15 minutes.
- Do not log passwords, refresh tokens, access tokens, or OAuth provider credentials.
- Use environment-specific OAuth2 client credentials outside source control.
- Keep Sentry DSNs and auth tokens outside source control.

## Backup And Recovery

Production should define backups for PostgreSQL data volumes before handling real user data. At minimum:

- Schedule database backups.
- Test restore procedures.
- Document retention windows.
- Keep deployment artifacts tied to Git SHAs.

## Production Readiness Checklist

- Add HTTPS in front of Kong Gateway.
- Add centralized structured logs.
- Add secret management outside plain environment files.
- Add rate limiting for auth endpoints.
- Add distributed tracing.
- Add authenticated alert routes for production incident channels.
