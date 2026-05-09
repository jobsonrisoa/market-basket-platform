package com.jobson.market.auth.infrastructure.persistence;

import com.jobson.market.auth.application.port.token.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

  private static final String ACTIVE_STATUS = "ACTIVE";

  private final SpringDataRefreshTokenFamilyRepository families;
  private final SpringDataRefreshTokenRepository tokens;
  private final Clock clock;

  JpaRefreshTokenRepository(
      SpringDataRefreshTokenFamilyRepository families,
      SpringDataRefreshTokenRepository tokens,
      Clock clock) {
    this.families = families;
    this.tokens = tokens;
    this.clock = clock;
  }

  @Override
  public UUID createFamily(UUID userId) {
    UUID familyId = UUID.randomUUID();
    families.save(new RefreshTokenFamilyEntity(familyId, userId, ACTIVE_STATUS, clock.instant()));
    return familyId;
  }

  @Override
  public IssuedRefreshToken saveToken(
      UUID familyId, UUID userId, String tokenHash, UUID previousTokenId, Instant expiresAt) {
    UUID tokenId = UUID.randomUUID();
    RefreshTokenEntity token =
        tokens.save(
            new RefreshTokenEntity(
                tokenId, familyId, userId, tokenHash, previousTokenId, expiresAt, clock.instant()));
    return new IssuedRefreshToken(token.id(), token.familyId(), token.userId(), token.expiresAt());
  }

  @Override
  public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
    return tokens.findByTokenHash(tokenHash).map(this::toStored);
  }

  @Override
  public void markUsed(UUID tokenId, Instant usedAt) {
    tokens
        .findById(tokenId)
        .ifPresent(
            token -> {
              token.markUsed(usedAt);
              tokens.save(token);
            });
  }

  @Override
  public void revokeToken(UUID tokenId, Instant revokedAt) {
    tokens
        .findById(tokenId)
        .ifPresent(
            token -> {
              token.revoke(revokedAt);
              tokens.save(token);
            });
  }

  @Override
  public void revokeFamily(UUID familyId, Instant revokedAt, String reason) {
    families
        .findById(familyId)
        .ifPresent(
            family -> {
              family.revoke(revokedAt, reason);
              families.save(family);
            });
    for (RefreshTokenEntity token : tokens.findByFamilyId(familyId)) {
      if (token.revokedAt() == null) {
        token.revoke(revokedAt);
        tokens.save(token);
      }
    }
  }

  @Override
  public void revokeAllFamiliesByUserId(UUID userId, Instant revokedAt, String reason) {
    for (RefreshTokenFamilyEntity family : families.findByUserIdAndStatus(userId, ACTIVE_STATUS)) {
      revokeFamily(family.id(), revokedAt, reason);
    }
  }

  @Override
  public boolean isFamilyActive(UUID familyId) {
    return families.findById(familyId).map(RefreshTokenFamilyEntity::active).orElse(false);
  }

  @Override
  public List<UUID> findActiveFamilyIdsByUserId(UUID userId) {
    return families.findByUserIdAndStatus(userId, ACTIVE_STATUS).stream()
        .map(RefreshTokenFamilyEntity::id)
        .toList();
  }

  private StoredRefreshToken toStored(RefreshTokenEntity token) {
    return new StoredRefreshToken(
        token.id(),
        token.familyId(),
        token.userId(),
        token.tokenHash(),
        token.previousTokenId(),
        token.expiresAt(),
        token.usedAt(),
        token.revokedAt());
  }
}
