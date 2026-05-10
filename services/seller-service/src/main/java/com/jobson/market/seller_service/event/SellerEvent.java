package com.jobson.market.seller_service.event;

import com.jobson.market.seller_service.domain.SellerStoreEntity;
import java.time.Instant;
import java.util.UUID;

public record SellerEvent(
    UUID eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    SellerApprovedPayload payload) {

  public static SellerEvent sellerApproved(SellerStoreEntity seller) {
    if (seller.reviewedAt() == null || seller.reviewedByUserId() == null) {
      throw new IllegalArgumentException("Approved seller review details are required");
    }
    return new SellerEvent(
        UUID.randomUUID(),
        "seller.approved.v1",
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        SellerApprovedPayload.from(seller));
  }

  public record SellerApprovedPayload(
      String sellerId,
      String name,
      String ownerUserId,
      String approvalStatus,
      String reviewedByUserId,
      Instant reviewedAt) {

    static SellerApprovedPayload from(SellerStoreEntity seller) {
      return new SellerApprovedPayload(
          seller.id().toString(),
          seller.name(),
          seller.ownerUserId().toString(),
          seller.approvalStatus().name(),
          seller.reviewedByUserId().toString(),
          seller.reviewedAt());
    }
  }
}
