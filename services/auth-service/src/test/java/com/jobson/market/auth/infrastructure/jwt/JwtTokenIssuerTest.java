package com.jobson.market.auth.infrastructure.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jobson.market.auth.application.port.crypto.RefreshTokenCodec;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.port.token.RefreshTokenRepository;
import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import com.jobson.market.auth.infrastructure.config.AuthProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtTokenIssuerTest {

  @Test
  void shouldIncludeRbacAndProfileClaims() {
    CapturingJwtEncoder jwtEncoder = new CapturingJwtEncoder();
    Clock clock = Clock.fixed(Instant.parse("2026-05-08T12:00:00Z"), ZoneOffset.UTC);
    JwtTokenIssuer issuer =
        new JwtTokenIssuer(
            jwtEncoder,
            refreshTokenService(clock),
            new AuthProperties(
                new AuthProperties.Jwt(
                    "market-auth", "market-services", Duration.ofMinutes(15), "test-key"),
                new AuthProperties.RefreshToken(Duration.ofDays(30))),
            clock);

    issuer.issueAccessToken(User.register(new Email("john@example.com")));

    assertEquals(List.of("CUSTOMER"), jwtEncoder.claims.get("roles"));
    assertEquals(
        List.of("CUSTOMER_PROFILE_ACCESS", "CUSTOMER_SUBSCRIPTION_MANAGE_OWN"),
        jwtEncoder.claims.get("permissions"));
    assertEquals("CUSTOMER", jwtEncoder.claims.get("account_profile"));
    assertEquals("INDIVIDUAL", jwtEncoder.claims.get("customer_profile_type"));
  }

  private static RefreshTokenService refreshTokenService(Clock clock) {
    return new RefreshTokenService(
        new FakeRefreshTokenRepository(),
        new FakeRefreshTokenCodec(),
        new EmptyUserRepository(),
        event -> {},
        clock,
        Duration.ofDays(30));
  }

  private static class CapturingJwtEncoder implements JwtEncoder {
    private Map<String, Object> claims;

    @Override
    public Jwt encode(JwtEncoderParameters parameters) {
      claims = parameters.getClaims().getClaims();
      return Jwt.withTokenValue("access-token")
          .headers(headers -> headers.putAll(parameters.getJwsHeader().getHeaders()))
          .claims(tokenClaims -> tokenClaims.putAll(claims))
          .build();
    }
  }

  private static class FakeRefreshTokenRepository implements RefreshTokenRepository {

    @Override
    public UUID createFamily(UUID userId) {
      return UUID.randomUUID();
    }

    @Override
    public IssuedRefreshToken saveToken(
        UUID familyId, UUID userId, String tokenHash, UUID previousTokenId, Instant expiresAt) {
      return new IssuedRefreshToken(UUID.randomUUID(), familyId, userId, expiresAt);
    }

    @Override
    public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
      return Optional.empty();
    }

    @Override
    public void markUsed(UUID tokenId, Instant usedAt) {
      // This fake never rotates tokens, so no used timestamp has to be tracked.
    }

    @Override
    public void revokeToken(UUID tokenId, Instant revokedAt) {
      // Token revocation is outside this JWT claim test.
    }

    @Override
    public void revokeFamily(UUID familyId, Instant revokedAt, String reason) {
      // Family revocation is outside this JWT claim test.
    }

    @Override
    public void revokeAllFamiliesByUserId(UUID userId, Instant revokedAt, String reason) {
      // Bulk revocation is outside this JWT claim test.
    }

    @Override
    public boolean isFamilyActive(UUID familyId) {
      return true;
    }

    @Override
    public List<UUID> findActiveFamilyIdsByUserId(UUID userId) {
      return List.of();
    }
  }

  private static class FakeRefreshTokenCodec implements RefreshTokenCodec {

    @Override
    public String generateRawToken() {
      return "refresh-token";
    }

    @Override
    public String hash(String rawToken) {
      return "hashed-" + rawToken;
    }
  }

  private static class EmptyUserRepository implements UserRepository {

    @Override
    public boolean existsByEmail(Email email) {
      return false;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return Optional.empty();
    }

    @Override
    public Optional<User> findById(UUID id) {
      return Optional.empty();
    }

    @Override
    public User save(User user) {
      return user;
    }
  }
}
