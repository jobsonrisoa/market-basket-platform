package com.jobson.market.seller_service.event;

import com.jobson.market.seller_service.domain.SellerApprovalStatus;
import com.jobson.market.seller_service.domain.SellerStoreEntity;
import java.time.Instant;
import java.util.UUID;

public record SellerEvent(
    UUID eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String correlationId,
    SellerReviewPayload payload) {

  public static SellerEvent sellerApproved(SellerStoreEntity seller) {
    if (seller.approvalStatus() != SellerApprovalStatus.APPROVED) {
      throw new IllegalArgumentException(
          "Seller must be approved before emitting seller.approved.v1");
    }
    if (seller.reviewedAt() == null || seller.reviewedByUserId() == null) {
      throw new IllegalArgumentException("Approved seller review details are required");
    }
    return new SellerEvent(
        UUID.randomUUID(),
        "seller.approved.v1",
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        SellerReviewPayload.from(seller));
  }

  public static SellerEvent sellerRejected(SellerStoreEntity seller) {
    if (seller.approvalStatus() != SellerApprovalStatus.REJECTED) {
      throw new IllegalArgumentException(
          "Seller must be rejected before emitting seller.rejected.v1");
    }
    if (seller.reviewedAt() == null || seller.reviewedByUserId() == null) {
      throw new IllegalArgumentException("Rejected seller review details are required");
    }
    return new SellerEvent(
        UUID.randomUUID(),
        "seller.rejected.v1",
        1,
        Instant.now(),
        UUID.randomUUID().toString(),
        SellerReviewPayload.from(seller));
  }

  public record SellerReviewPayload(
      String sellerId,
      String name,
      String ownerUserId,
      String approvalStatus,
      String reviewedByUserId,
      Instant reviewedAt) {

    static SellerReviewPayload from(SellerStoreEntity seller) {
      return new SellerReviewPayload(
          seller.id().toString(),
          seller.name(),
          seller.ownerUserId().toString(),
          seller.approvalStatus().name(),
          seller.reviewedByUserId().toString(),
          seller.reviewedAt());
    }
  }
}
