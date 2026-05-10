package com.jobson.market.seller_service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerStoreEntityTest {

  @Test
  void shouldCreateSellerStoreWithTrimmedName() {
    UUID ownerUserId = UUID.randomUUID();

    SellerStoreEntity store =
        SellerStoreEntity.create(
            "  Fresh Market  ", ownerUserId, Instant.parse("2026-05-10T12:00:00Z"));

    assertEquals("Fresh Market", store.name());
    assertEquals(ownerUserId, store.ownerUserId());
  }

  @Test
  void shouldRejectInvalidSellerStore() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SellerStoreEntity.create(" ", UUID.randomUUID(), Instant.now()));
    assertThrows(
        IllegalArgumentException.class,
        () -> SellerStoreEntity.create("Fresh Market", null, Instant.now()));
  }
}
