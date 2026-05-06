package com.jobson.market.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UUID familyId,
    String tokenHash,
    Instant issuedAt,
    Instant expiresAt,
    Instant revokedAt) {

  public RefreshToken {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(familyId, "familyId is required");
    if (tokenHash == null || tokenHash.isBlank()) {
      throw new IllegalArgumentException("tokenHash is required");
    }
    Objects.requireNonNull(issuedAt, "issuedAt is required");
    Objects.requireNonNull(expiresAt, "expiresAt is required");
    if (!expiresAt.isAfter(issuedAt)) {
      throw new IllegalArgumentException("expiresAt must be after issuedAt");
    }
  }

  public static RefreshToken issue(
      UUID familyId, UUID id, String tokenHash, Instant issuedAt, Instant expiresAt) {
    return new RefreshToken(id, familyId, tokenHash, issuedAt, expiresAt, null);
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isActive(Instant now) {
    return !isExpired(now) && !isRevoked();
  }

  public RefreshToken revoke(Instant revokedAt) {
    return new RefreshToken(id, familyId, tokenHash, issuedAt, expiresAt, revokedAt);
  }
}
