package com.jobson.market.auth.application.port.token;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

  UUID createFamily(UUID userId);

  IssuedRefreshToken saveToken(
      UUID familyId, UUID userId, String tokenHash, UUID previousTokenId, Instant expiresAt);

  Optional<StoredRefreshToken> findByTokenHash(String tokenHash);

  void markUsed(UUID tokenId, Instant usedAt);

  void revokeToken(UUID tokenId, Instant revokedAt);

  void revokeFamily(UUID familyId, Instant revokedAt, String reason);

  void revokeAllFamiliesByUserId(UUID userId, Instant revokedAt, String reason);

  boolean isFamilyActive(UUID familyId);

  List<UUID> findActiveFamilyIdsByUserId(UUID userId);

  record IssuedRefreshToken(UUID tokenId, UUID familyId, UUID userId, Instant expiresAt) {}

  record StoredRefreshToken(
      UUID tokenId,
      UUID familyId,
      UUID userId,
      String tokenHash,
      UUID previousTokenId,
      Instant expiresAt,
      Instant usedAt,
      Instant revokedAt) {

    public boolean isExpired(Instant now) {
      return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
      return usedAt != null;
    }

    public boolean isRevoked() {
      return revokedAt != null;
    }
  }
}
