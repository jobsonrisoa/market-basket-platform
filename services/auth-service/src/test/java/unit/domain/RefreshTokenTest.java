package unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.model.RefreshToken;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  void shouldKnowWhenTokenIsActive() {
    Instant now = Instant.parse("2026-05-05T18:00:00Z");
    RefreshToken token =
        RefreshToken.issue(
            UUID.randomUUID(), UUID.randomUUID(), "hash-123", now, now.plusSeconds(60));

    assertEquals("hash-123", token.tokenHash());
    assertFalse(token.isExpired(now));
    assertFalse(token.isRevoked());
    assertTrue(token.isActive(now));
  }

  @Test
  void shouldKnowWhenTokenIsExpired() {
    Instant now = Instant.parse("2026-05-05T18:00:00Z");
    RefreshToken token =
        RefreshToken.issue(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "hash-123",
            now.minusSeconds(120),
            now.minusSeconds(60));

    assertTrue(token.isExpired(now));
    assertFalse(token.isActive(now));
  }

  @Test
  void shouldKnowWhenTokenIsRevoked() {
    Instant now = Instant.parse("2026-05-05T18:00:00Z");
    RefreshToken token =
        RefreshToken.issue(
            UUID.randomUUID(), UUID.randomUUID(), "hash-123", now, now.plusSeconds(60));

    RefreshToken revokedToken = token.revoke(now.plusSeconds(10));

    assertTrue(revokedToken.isRevoked());
    assertFalse(revokedToken.isActive(now.plusSeconds(10)));
  }
}
