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
  "emailVerified": false
}
```

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
- Refresh tokens are opaque values and should be handled as secrets.
- Login and refresh responses include refresh tokens in both response body data and an HTTP-only cookie.
- The cookie is marked `Secure`, so browser-based local testing over plain HTTP may need direct request-body refresh handling unless local HTTPS is introduced.
