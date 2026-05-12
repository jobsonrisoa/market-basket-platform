package com.jobson.market.customer_service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerProfileEntityTest {

  @Test
  void shouldCreateActiveProfileForRegisteredUser() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.parse("2026-05-10T12:00:00Z");

    CustomerProfileEntity profile = CustomerProfileEntity.initialFor(userId, now);

    assertEquals(userId, profile.authUserId());
    assertEquals("en-US", profile.defaultLocale());
    assertEquals(CustomerProfileStatus.ACTIVE, profile.status());
    assertEquals(now, profile.createdAt());
    assertEquals(now, profile.updatedAt());
  }

  @Test
  void shouldTrimAndNormalizeProfileUpdates() {
    CustomerProfileEntity profile =
        CustomerProfileEntity.initialFor(UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));

    profile.update(
        "  John Market  ", "  +15551234567  ", " pt-BR ", Instant.parse("2026-05-11T12:00:00Z"));

    assertEquals("John Market", profile.displayName());
    assertEquals("+15551234567", profile.phone());
    assertEquals("pt-BR", profile.defaultLocale());
    assertEquals(Instant.parse("2026-05-11T12:00:00Z"), profile.updatedAt());
  }

  @Test
  void shouldRequireAuthUserId() {
    Instant now = Instant.parse("2026-05-10T12:00:00Z");

    assertThrows(IllegalArgumentException.class, () -> CustomerProfileEntity.initialFor(null, now));
  }
}
