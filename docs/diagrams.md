# Diagrams

These diagrams use Mermaid and render directly in GitHub.

## System Context

```mermaid
flowchart LR
  shopper[Shopper or Client App]
  operator[Store Operator]
  platform[Market Basket Platform]
  ghcr[GitHub Container Registry]
  deployHost[Docker Compose Host]

  shopper -->|HTTPS/API requests| platform
  operator -->|Back-office API requests| platform
  platform -->|Publishes images| ghcr
  ghcr -->|Images pulled by deploy| deployHost
```

## Container View

```mermaid
flowchart TB
  client[Client Applications]

  subgraph services[Spring Boot Services]
    auth[auth-service :8080]
    customer[customer-service :8081]
    catalog[catalog-service :8082]
    subscription[subscription-service :8083]
    order[order-service :8084]
    inventory[inventory-service :8085]
    notification[notification-service :8086]
  end

  subgraph data[Data and Messaging]
    postgres[(PostgreSQL)]
    redis[(Redis)]
    kafka[(Kafka)]
    mongo[(MongoDB provisioned)]
  end

  subgraph obs[Observability]
    prometheus[Prometheus :9090]
    grafana[Grafana :3000]
  end

  client --> auth
  client --> customer
  client --> catalog
  client --> subscription
  client --> order
  client --> inventory

  auth --> postgres
  customer --> postgres
  catalog --> postgres
  subscription --> postgres
  order --> postgres
  inventory --> postgres
  notification --> postgres

  auth --> redis
  customer --> redis
  catalog --> redis
  subscription --> redis
  order --> redis
  inventory --> redis
  notification --> redis

  auth --> kafka
  customer --> kafka
  catalog --> kafka
  subscription --> kafka
  order --> kafka
  inventory --> kafka
  notification --> kafka

  prometheus -. scrape .-> services
  grafana --> prometheus
```

## Auth Service Components

```mermaid
flowchart TB
  controller[AuthController and JWKS Controller]
  security[Spring Security]
  usecases[Application Use Cases]
  domain[Domain Model]
  ports[Application Ports]
  jpa[JPA Repositories]
  jwt[JWT Issuer and Key Store]
  crypto[Password and Refresh Token Crypto]
  outbox[Outbox Repository and Kafka Publisher]
  db[(market_auth)]
  kafka[(Kafka)]

  controller --> security
  controller --> usecases
  security --> jwt
  usecases --> domain
  usecases --> ports
  ports --> jpa
  ports --> jwt
  ports --> crypto
  ports --> outbox
  jpa --> db
  outbox --> db
  outbox --> kafka
```

## Login And Refresh Flow

```mermaid
sequenceDiagram
  participant Client
  participant AuthController
  participant LoginUseCase
  participant UserRepo
  participant PasswordCrypto
  participant TokenIssuer
  participant RefreshRepo

  Client->>AuthController: POST /auth/login
  AuthController->>LoginUseCase: email, password
  LoginUseCase->>UserRepo: find user and credential
  LoginUseCase->>PasswordCrypto: verify password
  PasswordCrypto-->>LoginUseCase: valid
  LoginUseCase->>TokenIssuer: issue JWT access token
  LoginUseCase->>RefreshRepo: create token family and hashed refresh token
  LoginUseCase-->>AuthController: access token and refresh token
  AuthController-->>Client: 200 OK plus refresh_token cookie

  Client->>AuthController: POST /auth/refresh
  AuthController->>LoginUseCase: refresh token
  LoginUseCase->>RefreshRepo: find by token hash
  LoginUseCase->>RefreshRepo: mark old token used and store new hashed token
  LoginUseCase->>TokenIssuer: issue new JWT access token
  LoginUseCase-->>AuthController: new access token and refresh token
  AuthController-->>Client: 200 OK plus rotated refresh_token cookie
```

## CI/CD Flow

```mermaid
flowchart LR
  pr[Pull Request to main]
  push[Push to main]
  ci[CI matrix: Spotless, package, reports, jars]
  docker[Docker Images matrix: build and push]
  ghcr[GHCR images tagged SHA and main]
  dev[Deploy Dev over SSH]
  prod[Manual Deploy Prod over SSH]

  pr --> ci
  push --> ci
  push --> docker
  docker --> ghcr
  docker -->|workflow_run success| dev
  ghcr --> dev
  ghcr --> prod
```

## Deployment Runtime

```mermaid
flowchart TB
  github[GitHub Actions Runner]
  host[Deployment Host]
  repo[Checked-out repository]
  compose[Docker Compose]
  registry[GitHub Container Registry]
  stack[Running platform containers]

  github -->|SSH with DEPLOY_* secrets| host
  host --> repo
  repo -->|git pull main| repo
  repo --> compose
  compose -->|docker compose pull| registry
  compose -->|docker compose up -d --remove-orphans| stack
```
