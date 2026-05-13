# API Reference

This document covers the endpoints currently implemented in the repository. Auth, customer, seller, catalog, and inventory have domain controllers. Subscription, order, and notification currently expose only framework and Actuator behavior, plus contract-test groundwork where present.

## Auth Service

Base URL in local Compose: `http://localhost:8080`

### Register

```http
POST /auth/register
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "StrongerPassword123!"
}
```

Response: `201 Created`

Returns the registration result produced by `RegisterUserUseCase`.
The result includes the user id, normalized email, roles, permissions, account profile, customer profile type, and email verification state.

### Login

```http
POST /auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "StrongerPassword123!"
}
```

Response: `200 OK`

Returns an access token and refresh token result produced by `LoginWithPasswordUseCase`. The response also sets an HTTP-only `refresh_token` cookie scoped to `/auth`.
The result includes roles, permissions, account profile, customer profile type, and token metadata used by clients for role-aware navigation.

### Refresh

```http
POST /auth/refresh
Content-Type: application/json
```

Request:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response: `200 OK`

Returns a new access token and rotated refresh token. The response also updates the HTTP-only `refresh_token` cookie scoped to `/auth`.

### Logout Current Refresh Token

```http
POST /auth/logout
Content-Type: application/json
```

Request:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response: `204 No Content`

Revokes the supplied refresh token.

### Logout All Sessions

```http
POST /auth/logout-all
Authorization: Bearer <access-token>
```

Response: `204 No Content`

Revokes all refresh-token families for the authenticated user.

### Current User

```http
GET /auth/me
Authorization: Bearer <access-token>
```

Response: `200 OK`

```json
{
  "userId": "user-uuid",
  "email": "user@example.com",
  "emailVerified": false,
  "roles": ["CUSTOMER"],
  "permissions": ["CUSTOMER_PROFILE_ACCESS", "CUSTOMER_SUBSCRIPTION_MANAGE_OWN"],
  "accountProfile": "CUSTOMER",
  "customerProfileType": "INDIVIDUAL"
}
```

### Assign User Role

```http
POST /auth/admin/users/{userId}/roles
Authorization: Bearer <access-token>
Content-Type: application/json
```

Requires `AUTH_USER_ROLE_ASSIGN`.

Request:

```json
{
  "role": "SELLER_OWNER"
}
```

Response: `204 No Content`

Assigns the requested role to the target user. Assigning security-sensitive roles such as `ADMIN` or `SUPER_ADMIN` requires a `SUPER_ADMIN` actor.

### Remove User Role

```http
DELETE /auth/admin/users/{userId}/roles/{role}
Authorization: Bearer <access-token>
```

Requires `AUTH_USER_ROLE_REVOKE`.

Response: `204 No Content`

Removes the requested role from the target user. Removing security-sensitive roles such as `ADMIN` or `SUPER_ADMIN` requires a `SUPER_ADMIN` actor.

### Suspend User

```http
POST /auth/admin/users/{userId}/suspend
Authorization: Bearer <access-token>
```

Requires `PLATFORM_SELLER_REVIEW`.

Response: `204 No Content`

Suspends the target user account.

### Reactivate User

```http
POST /auth/admin/users/{userId}/reactivate
Authorization: Bearer <access-token>
```

Requires `PLATFORM_SELLER_REVIEW`.

Response: `204 No Content`

Reactivates the target user account.

### JWKS

```http
GET /.well-known/jwks.json
```

Response: `200 OK`

Returns public JSON Web Key Set data for validating auth-service-issued JWT access tokens.

### Google OAuth2 Entry Points

```http
GET /oauth2/authorization/google
GET /login/oauth2/code/google
```

These are provided by Spring Security OAuth2 client configuration and the auth-service success handler. They are public entry points for the Google login flow when Google client credentials are configured.

### Health

```http
GET /actuator/health
```

Response: `200 OK`

The health endpoint is public. Other Actuator endpoints follow Spring Security rules.

## Auth Error Behavior

The auth service includes `AuthExceptionHandler` for translating application exceptions into HTTP responses. Keep error messages generic for credential failures so clients cannot infer whether an email exists.

## Authentication Notes

- Access tokens are bearer JWTs.
- JWTs include role, permission, account profile, customer profile type, and email verification claims.
- Refresh tokens are opaque values and should be handled as secrets.
- Login and refresh responses include refresh tokens in both response body data and an HTTP-only cookie.
- The cookie is marked `Secure`, so browser-based local testing over plain HTTP may need direct request-body refresh handling unless local HTTPS is introduced.

## Seller Service

Base URL in local Compose: `http://localhost:8087`

Seller business endpoints require `Authorization: Bearer <access-token>`.
Store reads require an active seller membership or platform admin role. Membership management requires
an active seller `OWNER` membership or platform admin role. Seller review endpoints require `ADMIN`
or `SUPER_ADMIN`.

Protected seller operations use the JWT subject as the actor user id. Request-body actor ids such as
`ownerUserId` and `reviewerUserId` are ignored when present and are retained only for temporary
backward compatibility.

### Create Seller

```http
POST /sellers
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "name": "Fresh Market"
}
```

Response: `201 Created`

Creates a seller store and an active `OWNER` membership for the authenticated JWT subject.
`ownerUserId` may still be accepted temporarily for backward compatibility but is ignored.

### Get Seller

```http
GET /sellers/{sellerId}
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns the seller store id, name, owner user id, approval status, submission timestamp, and
review details when present.

### Approve Seller

```http
POST /sellers/{sellerId}/approve
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "reviewNotes": "Ready for marketplace"
}
```

Response: `200 OK`

Marks the seller store as `APPROVED`. Seller-service publishes `seller.approved.v1` to Kafka after
the store is approved. Catalog-service consumes that event into its seller approval read model.
The reviewer user is the authenticated JWT subject. `reviewerUserId` may still be accepted
temporarily for backward compatibility but is ignored.

### Reject Seller

```http
POST /sellers/{sellerId}/reject
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "reviewNotes": "Missing license"
}
```

Response: `200 OK`

Marks the seller store as `REJECTED` with reviewer metadata from the authenticated JWT subject.
Seller-service publishes `seller.rejected.v1` to Kafka after the store is rejected. Catalog-service
consumes that event into its seller approval read model so future product publish attempts fail
closed for the seller.

### Add Seller Member

```http
POST /sellers/{sellerId}/members
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "userId": "user-uuid",
  "role": "STAFF"
}
```

Response: `201 Created`

Creates or reactivates a seller membership as `OWNER` or `STAFF`.

### List Seller Members

```http
GET /sellers/{sellerId}/members
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns active and removed memberships for the seller.

### Remove Seller Member

```http
DELETE /sellers/{sellerId}/members/{userId}
Authorization: Bearer <access-token>
```

Response: `204 No Content`

Marks the membership as `REMOVED`.

## Customer Service

Base URL in local Compose: `http://localhost:8081`

Customer profile endpoints require `Authorization: Bearer <access-token>`. Customer self-service
endpoints use the JWT subject as the auth user id and require the `CUSTOMER` role. Support reads
require `SUPPORT_AGENT`, `ADMIN`, or `SUPER_ADMIN`.

Customer-service owns customer profile data keyed one-to-one by auth user id. It consumes
`auth.user.registered.v1` from auth-service to create initial profiles idempotently; duplicate
registration events leave the existing profile unchanged.

### Get Current Customer Profile

```http
GET /customers/me/profile
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns the authenticated customer's profile id, auth user id, display name, phone, default locale,
profile status, and timestamps.

### Update Current Customer Profile

```http
PATCH /customers/me/profile
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "displayName": "Jane Market",
  "phone": "+15551234567",
  "defaultLocale": "pt-BR"
}
```

Response: `200 OK`

Updates only the authenticated customer's own editable profile fields.

### Support Read Customer Profile

```http
GET /customers/profiles/{authUserId}
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns the profile for support and platform operations.

### Support List Customer Profiles

```http
GET /customers/profiles
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns customer profiles for platform support workflows.

## Catalog Service

Base URL in local Compose: `http://localhost:8082`

Catalog browsing endpoints are public. Catalog management endpoints require `Authorization: Bearer <access-token>` with a seller or admin role.
Product write endpoints also require platform admin role or an active seller membership claim for
the target seller. Publishing a product additionally requires catalog-service to have an
`APPROVED` seller approval state for the product seller from seller review events.

Seller membership claims are expected in JWTs for seller-scoped catalog writes:

```json
{
  "seller_memberships": [
    {"sellerId": "seller-uuid", "role": "OWNER", "status": "ACTIVE"}
  ]
}
```

`role` is currently advisory for downstream services; authorization requires a matching `sellerId`
with `status` equal to `ACTIVE`. Seller-service remains the source of truth for membership records.

### Create Category

```http
POST /catalog/categories
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "name": "Produce"
}
```

Response: `201 Created`

Creates a service-owned catalog category.

### List Categories

```http
GET /catalog/categories
```

Response: `200 OK`

Returns categories ordered by name.

### Get Category

```http
GET /catalog/categories/{categoryId}
```

Response: `200 OK`

Returns the category id, name, and timestamps.

### Rename Category

```http
PATCH /catalog/categories/{categoryId}
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "name": "Seasonal Produce"
}
```

Response: `200 OK`

Renames the category.

### Create Product

```http
POST /catalog/products
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "sellerId": "seller-uuid",
  "categoryId": "category-uuid",
  "name": "Organic Carrots",
  "description": "Fresh carrots",
  "unit": "kg",
  "packageSize": "1 kg bag",
  "priceAmount": 7.50,
  "currency": "USD"
}
```

Response: `201 Created`

Creates a seller-owned product in `DRAFT` status.

### Get Product

```http
GET /catalog/products/{productId}
```

Response: `200 OK`

Returns product details, price, lifecycle status, and timestamps.

### List Seller Products

```http
GET /catalog/products?sellerId={sellerId}&status=DRAFT
```

Response: `200 OK`

Returns products for the seller. The `status` query parameter is optional.

### Update Product

```http
PATCH /catalog/products/{productId}
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "categoryId": "category-uuid",
  "name": "Rainbow Carrots",
  "description": "Colorful carrots",
  "unit": "bundle",
  "packageSize": "6 count",
  "priceAmount": 9.25,
  "currency": "BRL"
}
```

Response: `200 OK`

Updates product details while preserving the product's seller owner.

### Publish Product

```http
POST /catalog/products/{productId}/publish
Authorization: Bearer <access-token>
```

Response: `200 OK`

Changes the product status to `PUBLISHED` only when the seller approval read model says the product
seller is `APPROVED`. If approval data is missing, pending, rejected, or otherwise not approved,
the endpoint returns `409 Conflict` with `Seller must be approved before publishing products`.
Catalog-service also has a JSON Schema producer contract for `catalog.product.published.v1`;
runtime Kafka publishing is deferred.

### Unpublish Product

```http
POST /catalog/products/{productId}/unpublish
Authorization: Bearer <access-token>
```

Response: `200 OK`

Changes the product status to `UNPUBLISHED`.

## Inventory Service

Base URL in local Compose: `http://localhost:8085`

Inventory APIs require `Authorization: Bearer <access-token>` with `SELLER_OWNER`, `SELLER_STAFF`, `ADMIN`, `SUPER_ADMIN`, or `SERVICE` role. The auth service currently issues marketplace user/admin roles; `SERVICE` is accepted by inventory authorization for future service-to-service tokens but is not part of the current auth role model.
Seller user stock and reservation access also requires an active seller membership claim for the
stock's seller. `ADMIN`, `SUPER_ADMIN`, and `SERVICE` bypass seller membership checks for platform
support and service-to-service workflows. Catalog product ownership lookups remain future scope.

### Upsert Stock

```http
POST /inventory/stocks
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "sellerId": "seller-uuid",
  "productId": "product-uuid",
  "onHandQuantity": 25.5,
  "unit": "kg"
}
```

Response: `201 Created`

Creates or replaces the stock quantity for a seller/product pair. Availability is derived as
`onHandQuantity - reservedQuantity`.

### Get Stock

```http
GET /inventory/stocks/{stockId}
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns on-hand, reserved, and available quantities for the stock record.

### List Seller Stock

```http
GET /inventory/stocks?sellerId={sellerId}
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns stock records owned by the seller.

### Reserve Stock

```http
POST /inventory/reservations
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "stockId": "stock-uuid",
  "quantity": 4.5,
  "requestedBy": "order-service",
  "referenceId": "order-123",
  "expiresAt": "2026-05-10T14:00:00Z"
}
```

Response: `201 Created`

Creates an `ACTIVE` reservation and reduces available stock. Repeating the same `stockId`,
`requestedBy`, and `referenceId` returns the existing reservation without reserving stock again.
`expiresAt` is optional. Inventory-service has a JSON Schema producer contract for
`inventory.stock_reserved.v1`; runtime Kafka publishing is deferred.

### Release Reservation

```http
POST /inventory/reservations/{reservationId}/release
Authorization: Bearer <access-token>
```

Response: `200 OK`

Marks an active reservation as `RELEASED` and returns its quantity to availability. Inventory-service
has a JSON Schema producer contract for `inventory.reservation_released.v1`; runtime Kafka publishing
is deferred.

### Release Reservation By Reference

```http
POST /inventory/reservations/release
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "stockId": "stock-uuid",
  "requestedBy": "order-service",
  "referenceId": "order-123"
}
```

Response: `200 OK`

Idempotently releases the matching active reservation. Repeated calls return the terminal reservation
without changing stock again.

### Commit Reservation

```http
POST /inventory/reservations/commit
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "stockId": "stock-uuid",
  "requestedBy": "order-service",
  "referenceId": "order-123"
}
```

Response: `200 OK`

Idempotently marks an active reservation as `COMMITTED`, reduces reserved quantity, and reduces
on-hand quantity exactly once. Inventory-service has a JSON Schema producer contract for
`inventory.reservation_committed.v1`; runtime Kafka publishing is deferred.

### Expire Reservations

```http
POST /inventory/reservations/expire
Authorization: Bearer <access-token>
```

Response: `200 OK`

Returns the reservations expired by the command. Active reservations whose `expiresAt` is at or
before the service clock are marked `EXPIRED` and returned to availability exactly once.
This command requires `SERVICE`, `ADMIN`, or `SUPER_ADMIN`.
Inventory-service has a JSON Schema producer contract for `inventory.reservation_expired.v1`;
runtime Kafka publishing is deferred.

### Adjust Stock

```http
POST /inventory/stocks/{stockId}/adjustments
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "quantityDelta": -2.5,
  "reason": "shrinkage",
  "referenceId": "cycle-count-1"
}
```

Response: `200 OK`

Applies a forward-only on-hand correction. The adjustment may not make on-hand quantity negative or
lower than currently reserved quantity. Inventory-service has a JSON Schema producer contract for
`inventory.stock_adjusted.v1`; runtime Kafka publishing is deferred.
