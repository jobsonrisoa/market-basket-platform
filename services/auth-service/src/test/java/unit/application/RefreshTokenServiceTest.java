package unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobson.market.auth.application.port.crypto.RefreshTokenCodec;
import com.jobson.market.auth.application.port.event.OutboxEventRepository;
import com.jobson.market.auth.application.port.identity.UserRepository;
import com.jobson.market.auth.application.port.token.AccessTokenIssuer;
import com.jobson.market.auth.application.port.token.RefreshTokenRepository;
import com.jobson.market.auth.application.service.RefreshTokenService;
import com.jobson.market.auth.application.usecase.authentication.InvalidRefreshTokenException;
import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.AuthTokens;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

  private final Instant now = Instant.parse("2026-05-08T12:00:00Z");
  private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

  @Test
  void shouldIssueInitialRefreshToken() {
    User user = User.register(new Email("john@example.com"));
    FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
    RefreshTokenService service = newService(refreshTokens, new FakeUserRepository(user));

    RefreshTokenService.IssuedRawRefreshToken issued = service.issueInitial(user);

    assertEquals("raw-token-1", issued.rawToken());
    assertEquals(refreshTokens.familyId, issued.familyId());
    assertEquals(now.plus(Duration.ofDays(30)), issued.expiresAt());
  }

  @Test
  void shouldRotateRefreshTokenAndIssueNewAccessToken() {
    User user = User.register(new Email("john@example.com")).verifyEmail();
    FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
    UUID familyId = refreshTokens.createFamily(user.id());
    refreshTokens.saveToken(
        familyId, user.id(), "hash-current", null, now.plus(Duration.ofDays(1)));
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    RefreshTokenService service =
        newService(refreshTokens, new FakeUserRepository(user), outbox, new SequencedCodec());

    AuthTokens tokens = service.rotate("current");

    assertEquals("access-" + user.id(), tokens.accessToken());
    assertEquals("raw-token-1", tokens.refreshToken());
    assertEquals(now, refreshTokens.usedAt);
    assertEquals("auth.session.refresh_token_rotated.v1", outbox.events.get(0).eventType());
  }

  @Test
  void shouldRevokeFamilyWhenRefreshTokenReuseIsDetected() {
    User user = User.register(new Email("john@example.com")).verifyEmail();
    FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
    UUID familyId = refreshTokens.createFamily(user.id());
    refreshTokens.saveToken(
        familyId, user.id(), "hash-current", null, now.plus(Duration.ofDays(1)));
    refreshTokens.stored =
        new RefreshTokenRepository.StoredRefreshToken(
            refreshTokens.stored.tokenId(),
            familyId,
            user.id(),
            "hash-current",
            null,
            now.plus(Duration.ofDays(1)),
            now.minusSeconds(10),
            null);
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    RefreshTokenService service =
        newService(refreshTokens, new FakeUserRepository(user), outbox, new SequencedCodec());

    assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("current"));

    assertEquals("reuse_detected", refreshTokens.revocationReason);
    assertEquals("auth.session.refresh_token_reused.v1", outbox.events.get(0).eventType());
  }

  @Test
  void shouldRejectBlankAndExpiredRefreshToken() {
    User user = User.register(new Email("john@example.com")).verifyEmail();
    FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
    UUID familyId = refreshTokens.createFamily(user.id());
    refreshTokens.saveToken(familyId, user.id(), "hash-current", null, now.minusSeconds(1));
    RefreshTokenService service = newService(refreshTokens, new FakeUserRepository(user));

    assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(" "));
    assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("current"));
  }

  @Test
  void shouldRevokeCurrentSessionAndAllUserSessions() {
    User user = User.register(new Email("john@example.com")).verifyEmail();
    FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
    UUID familyId = refreshTokens.createFamily(user.id());
    refreshTokens.saveToken(
        familyId, user.id(), "hash-current", null, now.plus(Duration.ofDays(1)));
    FakeOutboxEventRepository outbox = new FakeOutboxEventRepository();
    RefreshTokenService service =
        newService(refreshTokens, new FakeUserRepository(user), outbox, new SequencedCodec());

    service.revokeSession("current");
    service.revokeAll(user.id());

    assertEquals("logout_all", refreshTokens.revocationReason);
    assertEquals(2, outbox.events.size());
    assertEquals("auth.session.revoked.v1", outbox.events.get(0).eventType());
    assertEquals("auth.session.revoked.v1", outbox.events.get(1).eventType());
  }

  private RefreshTokenService newService(
      FakeRefreshTokenRepository refreshTokens, FakeUserRepository users) {
    return newService(refreshTokens, users, new FakeOutboxEventRepository(), new SequencedCodec());
  }

  private RefreshTokenService newService(
      FakeRefreshTokenRepository refreshTokens,
      FakeUserRepository users,
      FakeOutboxEventRepository outbox,
      RefreshTokenCodec codec) {
    RefreshTokenService service =
        new RefreshTokenService(refreshTokens, codec, users, outbox, clock, Duration.ofDays(30));
    service.setAccessTokenIssuer(new FakeAccessTokenIssuer());
    return service;
  }

  private static class SequencedCodec implements RefreshTokenCodec {
    private int generated;

    @Override
    public String generateRawToken() {
      generated++;
      return "raw-token-" + generated;
    }

    @Override
    public String hash(String rawToken) {
      return "hash-" + rawToken;
    }
  }

  private static class FakeAccessTokenIssuer implements AccessTokenIssuer {
    @Override
    public String issueAccessToken(User user) {
      return "access-" + user.id();
    }
  }

  private static class FakeRefreshTokenRepository implements RefreshTokenRepository {
    private final Map<UUID, UUID> familyUsers = new HashMap<>();
    private UUID familyId;
    private StoredRefreshToken stored;
    private Instant usedAt;
    private String revocationReason;
    private boolean familyActive = true;

    @Override
    public UUID createFamily(UUID userId) {
      familyId = UUID.randomUUID();
      familyUsers.put(familyId, userId);
      return familyId;
    }

    @Override
    public IssuedRefreshToken saveToken(
        UUID familyId, UUID userId, String tokenHash, UUID previousTokenId, Instant expiresAt) {
      UUID tokenId = UUID.randomUUID();
      stored =
          new StoredRefreshToken(
              tokenId, familyId, userId, tokenHash, previousTokenId, expiresAt, null, null);
      return new IssuedRefreshToken(tokenId, familyId, userId, expiresAt);
    }

    @Override
    public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
      return Optional.ofNullable(stored).filter(token -> token.tokenHash().equals(tokenHash));
    }

    @Override
    public void markUsed(UUID tokenId, Instant usedAt) {
      this.usedAt = usedAt;
    }

    @Override
    public void revokeToken(UUID tokenId, Instant revokedAt) {
      // The service under test revokes token families, not individual tokens.
    }

    @Override
    public void revokeFamily(UUID familyId, Instant revokedAt, String reason) {
      familyActive = false;
      revocationReason = reason;
    }

    @Override
    public void revokeAllFamiliesByUserId(UUID userId, Instant revokedAt, String reason) {
      revocationReason = reason;
      familyActive = false;
    }

    @Override
    public boolean isFamilyActive(UUID familyId) {
      return familyActive;
    }

    @Override
    public List<UUID> findActiveFamilyIdsByUserId(UUID userId) {
      return familyUsers.entrySet().stream()
          .filter(entry -> entry.getValue().equals(userId))
          .map(Map.Entry::getKey)
          .toList();
    }
  }

  private static class FakeUserRepository implements UserRepository {
    private final User user;

    private FakeUserRepository(User user) {
      this.user = user;
    }

    @Override
    public boolean existsByEmail(Email email) {
      return user.email().equals(email);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
      return existsByEmail(email) ? Optional.of(user) : Optional.empty();
    }

    @Override
    public Optional<User> findById(UUID id) {
      return user.id().equals(id) ? Optional.of(user) : Optional.empty();
    }

    @Override
    public User save(User user) {
      return user;
    }
  }

  private static class FakeOutboxEventRepository implements OutboxEventRepository {
    private final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
      events.add(event);
    }
  }
}
