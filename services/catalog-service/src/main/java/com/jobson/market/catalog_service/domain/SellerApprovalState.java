package com.jobson.market.catalog_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_seller_approvals")
public class SellerApprovalState {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private String sellerName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SellerApprovalStatus approvalStatus;

  @Column(nullable = false)
  private UUID reviewedByUserId;

  @Column(nullable = false)
  private Instant reviewedAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected SellerApprovalState() {}

  public static SellerApprovalState reviewed(
      UUID sellerId,
      String sellerName,
      SellerApprovalStatus approvalStatus,
      UUID reviewedByUserId,
      Instant reviewedAt) {
    SellerApprovalState state = new SellerApprovalState();
    state.sellerId = Objects.requireNonNull(sellerId, "sellerId is required");
    state.sellerName = requireText(sellerName, "sellerName");
    state.approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus is required");
    state.reviewedByUserId =
        Objects.requireNonNull(reviewedByUserId, "reviewedByUserId is required");
    state.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt is required");
    state.updatedAt = reviewedAt;
    return state;
  }

  public void recordReview(
      String sellerName,
      SellerApprovalStatus approvalStatus,
      UUID reviewedByUserId,
      Instant reviewedAt) {
    this.sellerName = requireText(sellerName, "sellerName");
    this.approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus is required");
    this.reviewedByUserId =
        Objects.requireNonNull(reviewedByUserId, "reviewedByUserId is required");
    this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt is required");
    this.updatedAt = reviewedAt;
  }

  public UUID sellerId() {
    return sellerId;
  }

  public String sellerName() {
    return sellerName;
  }

  public SellerApprovalStatus approvalStatus() {
    return approvalStatus;
  }

  public UUID reviewedByUserId() {
    return reviewedByUserId;
  }

  public Instant reviewedAt() {
    return reviewedAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }
}
