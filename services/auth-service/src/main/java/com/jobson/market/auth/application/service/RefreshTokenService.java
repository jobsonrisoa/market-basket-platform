package com.jobson.market.auth.application.service;

import com.jobson.market.auth.application.port.AccessTokenIssuer;
import com.jobson.market.auth.application.port.OutboxEventRepository;
import com.jobson.market.auth.application.port.RefreshTokenCodec;
import com.jobson.market.auth.application.port.RefreshTokenRepository;
import com.jobson.market.auth.application.port.UserRepository;
import com.jobson.market.auth.application.usecase.InvalidRefreshTokenException;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokens;
  private final RefreshTokenCodec refreshTokenCodec;
  private final UserRepository users;
  private final OutboxEventRepository outbox;
  private final Clock clock;
  private final Duration refreshTokenTtl;
  private AccessTokenIssuer accessTokenIssuer;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokens,
      RefreshTokenCodec refreshTokenCodec,
      UserRepository users,
      OutboxEventRepository outbox,
      Clock clock,
      Duration refreshTokenTtl) {
    this.refreshTokens = Objects.requireNonNull(refreshTokens);
    this.refreshTokenCodec = Objects.requireNonNull(refreshTokenCodec);
    this.users = Objects.requireNonNull(users);
    this.outbox = Objects.requireNonNull(outbox);
    this.clock = Objects.requireNonNull(clock);
    this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl);
  }

  public void setAccessTokenIssuer(AccessTokenIssuer accessTokenIssuer) {
    this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
  }

  public IssuedRawRefreshToken issueInitial(User user) {
    UUID familyId = refreshTokens.createFamily(user.id());
    return issueRawRefreshToken(familyId, user.id(), null);
  }

  public AuthTokens rotate(String rawRefreshToken) {
    RefreshTokenRepository.StoredRefreshToken current = requireStoredToken(rawRefreshToken);
    Instant now = clock.instant();
    if (!refreshTokens.isFamilyActive(current.familyId())) {
      throw new InvalidRefreshTokenException();
    }
    if (current.isUsed()) {
      refreshTokens.revokeFamily(current.familyId(), now, "reuse_detected");
      outbox.save(OutboxEvent.refreshTokenReused(current.userId(), current.familyId()));
      throw new InvalidRefreshTokenException();
    }
    if (current.isExpired(now) || current.isRevoked()) {
      throw new InvalidRefreshTokenException();
    }

    refreshTokens.markUsed(current.tokenId(), now);
    IssuedRawRefreshToken rotated =
        issueRawRefreshToken(current.familyId(), current.userId(), current.tokenId());
    outbox.save(OutboxEvent.refreshTokenRotated(current.userId(), current.familyId()));
    User user = users.findById(current.userId()).orElseThrow(InvalidRefreshTokenException::new);
    return new AuthTokens(accessTokenIssuer.issueAccessToken(user), rotated.rawToken());
  }

  public void revokeSession(String rawRefreshToken) {
    RefreshTokenRepository.StoredRefreshToken current = requireStoredToken(rawRefreshToken);
    Instant now = clock.instant();
    refreshTokens.revokeFamily(current.familyId(), now, "logout");
    outbox.save(OutboxEvent.sessionRevoked(current.userId(), current.familyId()));
  }

  public void revokeAll(UUID userId) {
    Instant now = clock.instant();
    for (UUID familyId : refreshTokens.findActiveFamilyIdsByUserId(userId)) {
      outbox.save(OutboxEvent.sessionRevoked(userId, familyId));
    }
    refreshTokens.revokeAllFamiliesByUserId(userId, now, "logout_all");
  }

  private RefreshTokenRepository.StoredRefreshToken requireStoredToken(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new InvalidRefreshTokenException();
    }
    return refreshTokens
        .findByTokenHash(refreshTokenCodec.hash(rawRefreshToken))
        .orElseThrow(InvalidRefreshTokenException::new);
  }

  private IssuedRawRefreshToken issueRawRefreshToken(
      UUID familyId, UUID userId, UUID previousTokenId) {
    String rawToken = refreshTokenCodec.generateRawToken();
    RefreshTokenRepository.IssuedRefreshToken stored =
        refreshTokens.saveToken(
            familyId,
            userId,
            refreshTokenCodec.hash(rawToken),
            previousTokenId,
            clock.instant().plus(refreshTokenTtl));
    return new IssuedRawRefreshToken(
        rawToken, stored.tokenId(), stored.familyId(), stored.expiresAt());
  }

  public record IssuedRawRefreshToken(
      String rawToken, UUID tokenId, UUID familyId, Instant expiresAt) {}
}
