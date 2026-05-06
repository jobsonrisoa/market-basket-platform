package com.jobson.market.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TokenFamily(UUID id, UUID userId, TokenFamilyStatus status, Instant revokedAt) {

  public TokenFamily {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(userId, "userId is required");
    Objects.requireNonNull(status, "status is required");
  }

  public static TokenFamily start(UUID userId) {
    return new TokenFamily(UUID.randomUUID(), userId, TokenFamilyStatus.ACTIVE, null);
  }

  public boolean isRevoked() {
    return status != TokenFamilyStatus.ACTIVE;
  }

  public TokenFamily revokeBecauseOfReuse(Instant detectedAt) {
    Objects.requireNonNull(detectedAt, "detectedAt is required");
    return new TokenFamily(id, userId, TokenFamilyStatus.REVOKED_REUSE_DETECTED, detectedAt);
  }
}
