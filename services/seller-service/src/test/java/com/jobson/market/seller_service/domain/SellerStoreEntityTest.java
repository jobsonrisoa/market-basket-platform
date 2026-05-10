package com.jobson.market.seller_service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    assertEquals(SellerApprovalStatus.PENDING_REVIEW, store.approvalStatus());
    assertEquals(Instant.parse("2026-05-10T12:00:00Z"), store.submittedAt());
    assertNull(store.reviewedAt());
    assertNull(store.reviewedByUserId());
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

  @Test
  void shouldApproveSellerStore() {
    UUID reviewerUserId = UUID.randomUUID();
    SellerStoreEntity store =
        SellerStoreEntity.create(
            "Fresh Market", UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));

    store.approve(reviewerUserId, "Looks ready", Instant.parse("2026-05-10T13:00:00Z"));

    assertEquals(SellerApprovalStatus.APPROVED, store.approvalStatus());
    assertEquals(reviewerUserId, store.reviewedByUserId());
    assertEquals(Instant.parse("2026-05-10T13:00:00Z"), store.reviewedAt());
    assertEquals("Looks ready", store.reviewNotes());
  }

  @Test
  void shouldRejectSellerStore() {
    UUID reviewerUserId = UUID.randomUUID();
    SellerStoreEntity store =
        SellerStoreEntity.create(
            "Fresh Market", UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));

    store.reject(reviewerUserId, "Missing license", Instant.parse("2026-05-10T13:00:00Z"));

    assertEquals(SellerApprovalStatus.REJECTED, store.approvalStatus());
    assertEquals(reviewerUserId, store.reviewedByUserId());
    assertEquals(Instant.parse("2026-05-10T13:00:00Z"), store.reviewedAt());
    assertEquals("Missing license", store.reviewNotes());
  }
}
