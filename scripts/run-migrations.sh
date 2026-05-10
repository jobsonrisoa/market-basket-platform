#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

POSTGRES_USER="${POSTGRES_USER:-market}"
POSTGRES_DB="${POSTGRES_DB:-market}"

services=(
  migrate-auth-service
  migrate-customer-service
  migrate-seller-service
  migrate-catalog-service
  migrate-subscription-service
  migrate-order-service
  migrate-inventory-service
  migrate-notification-service
)

databases=(
  market_auth
  market_customer
  market_seller
  market_catalog
  market_subscription
  market_order
  market_inventory
  market_notification
)

docker compose up -d postgres

for attempt in {1..30}; do
  if docker compose exec -T postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" == "30" ]]; then
    echo "PostgreSQL did not become ready in time" >&2
    exit 1
  fi
  sleep 2
done

for database in "${databases[@]}"; do
  exists="$(
    docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
      "select 1 from pg_database where datname = '$database'"
  )"
  if [[ "$exists" != "1" ]]; then
    docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
      -c "create database $database"
  fi
done

for service in "${services[@]}"; do
  docker compose run --rm "$service" migrate
  docker compose run --rm "$service" validate
  docker compose run --rm "$service" info
done
