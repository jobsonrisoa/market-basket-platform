package unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobson.market.auth.domain.model.TokenFamily;
import com.jobson.market.auth.domain.model.TokenFamilyStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TokenFamilyTest {

  @Test
  void shouldCreateActiveTokenFamily() {
    TokenFamily family = TokenFamily.start(UUID.randomUUID());

    assertEquals(TokenFamilyStatus.ACTIVE, family.status());
    assertFalse(family.isRevoked());
  }

  @Test
  void shouldRevokeFamilyWhenRefreshTokenReuseIsDetected() {
    Instant detectedAt = Instant.parse("2026-05-05T18:00:00Z");
    TokenFamily family = TokenFamily.start(UUID.randomUUID());

    TokenFamily revokedFamily = family.revokeBecauseOfReuse(detectedAt);

    assertEquals(TokenFamilyStatus.REVOKED_REUSE_DETECTED, revokedFamily.status());
    assertTrue(revokedFamily.isRevoked());
    assertEquals(detectedAt, revokedFamily.revokedAt());
  }
}
