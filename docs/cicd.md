# CI/CD Flow

The repository uses GitHub Actions for pull request validation, image publication, and environment deployment.

## Workflows

| Workflow | File | Trigger | Purpose |
| --- | --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Pull request to `main`, push to `main`, manual dispatch | Validate migrations, format check, test, package, upload reports and jars for every service. |
| Docker Images | `.github/workflows/docker.yml` | Push to `main`, manual dispatch | Build and push every service image to GitHub Container Registry. |
| Deploy Dev | `.github/workflows/deploy-dev.yml` | Successful Docker Images workflow on `main`, manual dispatch | Deploy a specific image tag to the `dev` environment over SSH, then run smoke checks. |
| Deploy Prod | `.github/workflows/deploy-prod.yml` | Manual dispatch | Deploy an explicit image tag to the `prod` environment over SSH, then run smoke checks. |

## CI Matrix

CI runs two matrix jobs per service: one for migration validation and one for build/package validation.

- `auth-service`
- `customer-service`
- `seller-service`
- `catalog-service`
- `subscription-service`
- `order-service`
- `inventory-service`
- `notification-service`

For each service, the migration validation job:

1. Starts a disposable PostgreSQL 16 service.
2. Creates the service-owned database, for example `market_auth` or `market_catalog`.
3. Runs Flyway migrations through the service Maven wrapper.
4. Runs `flyway:validate` and `flyway:info` against the migrated database.

For each service, the build job:

1. Checks out the repository.
2. Sets up Temurin Java 17 with Maven cache.
3. Makes the service Maven wrapper executable.
4. Runs `./mvnw -B -ntp spotless:check`.
5. Runs `./mvnw -B -ntp package`.
6. Uploads Surefire reports.
7. Uploads the built service jar.

Application tests use Testcontainers for PostgreSQL, Redis, and Kafka. Since every service now uses Flyway and Hibernate validation, the package step also exercises startup-time migration behavior.

## Image Publishing

The Docker workflow publishes to `ghcr.io` using `GITHUB_TOKEN` package permissions.

Each image receives two tags:

- `${{ github.sha }}`
- `main`

Image names:

- `ghcr.io/<owner>/market-auth-service`
- `ghcr.io/<owner>/market-customer-service`
- `ghcr.io/<owner>/market-seller-service`
- `ghcr.io/<owner>/market-catalog-service`
- `ghcr.io/<owner>/market-subscription-service`
- `ghcr.io/<owner>/market-order-service`
- `ghcr.io/<owner>/market-inventory-service`
- `ghcr.io/<owner>/market-notification-service`

## Deployment

Both dev and prod deployments use `appleboy/ssh-action`.

The remote deployment script:

```bash
set -euo pipefail
cd "$DEPLOY_PATH"
git fetch origin main
git checkout main
git pull --ff-only origin main
export IMAGE_TAG="<git-sha>"
./scripts/run-migrations.sh
docker compose pull
docker compose up -d --remove-orphans
docker compose ps
curl --fail --retry 12 --retry-connrefused --retry-delay 5 http://localhost:8080/actuator/health
curl --fail --retry 12 --retry-connrefused --retry-delay 5 http://localhost:8000/.well-known/jwks.json
curl --fail --retry 12 --retry-connrefused --retry-delay 5 http://localhost:9090/-/ready
curl --fail --retry 12 --retry-connrefused --retry-delay 5 http://localhost:9090/api/v1/targets
curl --fail --retry 12 --retry-connrefused --retry-delay 5 http://localhost:9093/-/ready
```

`scripts/run-migrations.sh` starts PostgreSQL, waits for health, creates any missing service databases, and runs each service's pinned Flyway runner with `migrate`, `validate`, and `info`. It never runs `clean`. The deployment runners allow `baselineOnMigrate` so environments that already had pre-Flyway schemas can adopt Flyway history; CI validation remains strict against fresh databases. Application startup migrations remain enabled as a safety net after the controlled pre-rollout migration step.

Dev auto-deploy uses the Docker Images workflow head SHA as `IMAGE_TAG`. Manual dev and prod deployments require an `image_tag` input; production should use a Git SHA tag that was already published by the Docker Images workflow.

Dev deploys automatically after the Docker Images workflow succeeds on `main`. Prod deploys manually and should be protected by the GitHub `prod` environment.

Deployment smoke checks now verify container state, auth health, auth JWKS through Kong, Prometheus readiness, Prometheus target discovery, and Alertmanager readiness. Prometheus scrapes application Actuator metrics, Kong, PostgreSQL, Redis, and Kafka exporters from the deployed Compose network.

## Required Secrets

Configure these secrets in each GitHub environment that deploys:

| Secret | Purpose |
| --- | --- |
| `DEPLOY_HOST` | SSH host for the deployment target. |
| `DEPLOY_USER` | SSH username. |
| `DEPLOY_SSH_KEY` | Private key accepted by the deployment host. |
| `DEPLOY_PORT` | Optional SSH port. Defaults to `22` in workflow expressions. |
| `DEPLOY_PATH` | Absolute path to the checked-out repository on the host. |

## Environment Protection

Recommended GitHub environment rules:

- `dev`: allow automatic deployment from `main`.
- `prod`: require manual approval before the `Deploy Prod` job starts.

## Rollback

Deployments use the `IMAGE_TAG` environment variable, so rollback means redeploying a previously published Git SHA tag:

1. Set `IMAGE_TAG` on the deployment host to a known good SHA.
2. Run `docker compose pull`.
3. Run `docker compose up -d --remove-orphans`.
4. Verify `docker compose ps` and service health endpoints.

## Recommended Improvements

- Add a root script or parent build to run all service checks locally with one command.
- Add dependency vulnerability scanning.
- Add Docker image scanning.
