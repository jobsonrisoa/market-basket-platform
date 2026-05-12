# Diagrams

These diagrams use Mermaid and render directly in GitHub.

## System Context

```mermaid
flowchart LR
  shopper[Shopper or Client App]
  operator[Store Operator]
  platform[Market Basket Platform]
  gateway[Kong Gateway]
  ghcr[GitHub Container Registry]
  deployHost[Docker Compose Host]

  shopper -->|HTTPS/API requests| gateway
  operator -->|Back-office API requests| gateway
  gateway --> platform
  platform -->|Publishes images| ghcr
  ghcr -->|Images pulled by deploy| deployHost
```

## Container View

```mermaid
flowchart TB
  client[Client Applications]
  kong[Kong Gateway :8000]

  subgraph services[Spring Boot Services]
    auth[auth-service :8080]
    customer[customer-service :8081]
    seller[seller-service :8087]
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
    alertmanager[Alertmanager :9093]
    grafana[Grafana :3000]
    sonar[SonarQube :9000]
    exporters[Postgres, Redis, Kafka exporters]
  end

  client --> kong
  kong --> auth
  kong --> customer
  kong --> seller
  kong --> catalog
  kong --> subscription
  kong --> order
  kong --> inventory
  kong --> notification

  auth --> postgres
  customer --> postgres
  seller --> postgres
  catalog --> postgres
  subscription --> postgres
  order --> postgres
  inventory --> postgres
  notification --> postgres

  auth --> redis
  customer --> redis
  seller --> redis
  catalog --> redis
  subscription --> redis
  order --> redis
  inventory --> redis
  notification --> redis

  auth --> kafka
  customer --> kafka
  seller --> kafka
  catalog --> kafka
  subscription --> kafka
  order --> kafka
  inventory --> kafka
  notification --> kafka

  prometheus -. scrape .-> services
  prometheus -. scrape .-> kong
  prometheus -. scrape .-> exporters
  prometheus --> alertmanager
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

## Ownership Authorization Flow

```mermaid
sequenceDiagram
  participant Client
  participant Service as Customer/Seller/Catalog/Inventory Service
  participant JwtDecoder as JWKS JWT Decoder
  participant SellerDb as Seller Membership Store
  participant Domain as Domain Use Case

  Client->>Service: Protected write request plus bearer JWT
  Service->>JwtDecoder: Validate issuer, audience, signature
  JwtDecoder-->>Service: Subject, roles, permissions, seller_memberships

  alt customer profile self-service
    Service->>Domain: Use JWT subject as authUserId
    Domain-->>Service: Allow own profile read/update
  else SUPPORT_AGENT, ADMIN, or SUPER_ADMIN customer read
    Service->>Domain: Allow platform customer support read
  else ADMIN or SUPER_ADMIN
    Service->>Domain: Allow platform action
  else seller-service membership operation
    Service->>SellerDb: Load membership by sellerId and JWT subject
    SellerDb-->>Service: Membership role and status
    Service->>Domain: Allow only active OWNER membership
  else catalog/inventory seller-scoped operation
    Service->>Service: Match target sellerId to active seller_memberships claim
    Service->>Domain: Allow active matching seller membership
  else no active ownership
    Service-->>Client: 403 Forbidden
  end
```

## Customer Profile Creation Flow

```mermaid
sequenceDiagram
  participant Client
  participant Auth as auth-service
  participant Outbox as Auth Outbox Publisher
  participant Kafka
  participant Customer as customer-service
  participant CustomerDb as market_customer

  Client->>Auth: POST /auth/register
  Auth->>Auth: Create user and outbox event
  Outbox->>Kafka: auth.user.registered.v1
  Kafka-->>Customer: Deliver registration event
  Customer->>CustomerDb: Find profile by auth user id
  alt profile missing
    Customer->>CustomerDb: Insert ACTIVE customer profile
  else duplicate event
    Customer->>CustomerDb: Leave existing profile unchanged
  end
```

## CI/CD Flow

```mermaid
flowchart LR
  pr[Pull Request to main]
  push[Push to main]
  ci[CI matrix: Flyway validation, Spotless, package, reports, jars]
  docker[Docker Images matrix: build and push]
  migrations[Deploy migration runners]
  ghcr[GHCR images tagged SHA and main]
  dev[Deploy Dev over SSH]
  prod[Manual Deploy Prod over SSH]

  pr --> ci
  push --> ci
  push --> docker
  docker --> ghcr
  docker -->|workflow_run success| dev
  dev --> migrations
  prod --> migrations
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
  migrations[Flyway migration runners]
  registry[GitHub Container Registry]
  stack[Running platform containers]

  github -->|SSH with DEPLOY_* secrets| host
  host --> repo
  repo -->|git pull main| repo
  repo --> compose
  compose --> migrations
  compose -->|docker compose pull| registry
  compose -->|docker compose up -d --remove-orphans| stack
```
