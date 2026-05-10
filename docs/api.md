# API Reference

This document covers the endpoints currently implemented in the repository. Most non-auth services are scaffolds and expose only framework and Actuator behavior until domain controllers are added.

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
  "customerProfileType": "CUSTOMER"
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

These endpoints intentionally accept explicit user ids until service-to-service authorization is implemented.

### Create Seller

```http
POST /sellers
Content-Type: application/json
```

Request:

```json
{
  "name": "Fresh Market",
  "ownerUserId": "user-uuid"
}
```

Response: `201 Created`

Creates a seller store and an active `OWNER` membership for the owner user.

### Get Seller

```http
GET /sellers/{sellerId}
```

Response: `200 OK`

Returns the seller store id, name, owner user id, approval status, submission timestamp, and
review details when present.

### Approve Seller

```http
POST /sellers/{sellerId}/approve
Content-Type: application/json
```

Request:

```json
{
  "reviewerUserId": "admin-user-uuid",
  "reviewNotes": "Ready for marketplace"
}
```

Response: `200 OK`

Marks the seller store as `APPROVED`. Seller-service also has a JSON Schema producer contract for
`seller.approved.v1`; runtime Kafka publishing is deferred.

### Reject Seller

```http
POST /sellers/{sellerId}/reject
Content-Type: application/json
```

Request:

```json
{
  "reviewerUserId": "admin-user-uuid",
  "reviewNotes": "Missing license"
}
```

Response: `200 OK`

Marks the seller store as `REJECTED` with reviewer metadata.

### Add Seller Member

```http
POST /sellers/{sellerId}/members
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
```

Response: `200 OK`

Returns active and removed memberships for the seller.

### Remove Seller Member

```http
DELETE /sellers/{sellerId}/members/{userId}
```

Response: `204 No Content`

Marks the membership as `REMOVED`.

## Catalog Service

Base URL in local Compose: `http://localhost:8082`

These endpoints intentionally accept explicit seller ids until service-to-service authorization is implemented.

### Create Category

```http
POST /catalog/categories
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
```

Response: `200 OK`

Changes the product status to `PUBLISHED`. Catalog-service also has a JSON Schema producer contract for `catalog.product.published.v1`; runtime Kafka publishing is deferred.

### Unpublish Product

```http
POST /catalog/products/{productId}/unpublish
```

Response: `200 OK`

Changes the product status to `UNPUBLISHED`.

## Inventory Service

Base URL in local Compose: `http://localhost:8085`

These endpoints intentionally accept explicit seller and product ids until service-to-service
authorization and catalog lookups are implemented.

### Upsert Stock

```http
POST /inventory/stocks
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
```

Response: `200 OK`

Returns on-hand, reserved, and available quantities for the stock record.

### List Seller Stock

```http
GET /inventory/stocks?sellerId={sellerId}
```

Response: `200 OK`

Returns stock records owned by the seller.

### Reserve Stock

```http
POST /inventory/reservations
Content-Type: application/json
```

Request:

```json
{
  "stockId": "stock-uuid",
  "quantity": 4.5,
  "requestedBy": "order-service",
  "referenceId": "order-123"
}
```

Response: `201 Created`

Creates an `ACTIVE` reservation and reduces available stock. Inventory-service has a JSON Schema
producer contract for `inventory.stock_reserved.v1`; runtime Kafka publishing is deferred.

### Release Reservation

```http
POST /inventory/reservations/{reservationId}/release
```

Response: `200 OK`

Marks an active reservation as `RELEASED` and returns its quantity to availability. Inventory-service
has a JSON Schema producer contract for `inventory.reservation_released.v1`; runtime Kafka publishing
is deferred.
