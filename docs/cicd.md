# CI/CD Flow

The repository uses GitHub Actions for pull request validation, image publication, and environment deployment.

## Workflows

| Workflow | File | Trigger | Purpose |
| --- | --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Pull request to `main`, push to `main`, manual dispatch | Format check, test, package, upload reports and jars for every service. |
| Docker Images | `.github/workflows/docker.yml` | Push to `main`, manual dispatch | Build and push every service image to GitHub Container Registry. |
| Deploy Dev | `.github/workflows/deploy-dev.yml` | Successful Docker Images workflow on `main`, manual dispatch | Deploy the Compose stack to the `dev` environment over SSH. |
| Deploy Prod | `.github/workflows/deploy-prod.yml` | Manual dispatch | Deploy the Compose stack to the `prod` environment over SSH. |

## CI Matrix

CI runs one matrix entry per service:

- `auth-service`
- `customer-service`
- `seller-service`
- `catalog-service`
- `subscription-service`
- `order-service`
- `inventory-service`
- `notification-service`

For each service, CI:

1. Checks out the repository.
2. Sets up Temurin Java 17 with Maven cache.
3. Makes the service Maven wrapper executable.
4. Runs `./mvnw -B -ntp spotless:check`.
5. Runs `./mvnw -B -ntp package`.
6. Uploads Surefire reports.
7. Uploads the built service jar.

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
docker compose pull
docker compose up -d --remove-orphans
docker compose ps
```

Dev deploys automatically after the Docker Images workflow succeeds on `main`. Prod deploys manually and should be protected by the GitHub `prod` environment.

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

The current workflow deploys the `main` image tag by default through Compose. For stronger rollback control, prefer deploying immutable Git SHA tags:

1. Set `IMAGE_TAG` on the deployment host to a known good SHA.
2. Run `docker compose pull`.
3. Run `docker compose up -d --remove-orphans`.
4. Verify `docker compose ps` and service health endpoints.

## Recommended Improvements

- Add a root script or parent build to run all service checks locally with one command.
- Pin production deployments to SHA tags instead of mutable `main`.
- Add dependency vulnerability scanning.
- Add Docker image scanning.
- Add explicit migration validation checks, such as verifying Flyway info against each service database before deployment.
- Add smoke tests after dev and prod deployment.
