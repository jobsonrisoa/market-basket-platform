package com.jobson.market.seller_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_stores")
public class SellerStoreEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private UUID ownerUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SellerApprovalStatus approvalStatus;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant submittedAt;

  @Column private Instant reviewedAt;

  @Column private UUID reviewedByUserId;

  @Column private String reviewNotes;

  protected SellerStoreEntity() {}

  private SellerStoreEntity(
      UUID id,
      String name,
      UUID ownerUserId,
      SellerApprovalStatus approvalStatus,
      Instant createdAt,
      Instant submittedAt) {
    this.id = id;
    this.name = name;
    this.ownerUserId = ownerUserId;
    this.approvalStatus = approvalStatus;
    this.createdAt = createdAt;
    this.submittedAt = submittedAt;
  }

  public static SellerStoreEntity create(String name, UUID ownerUserId, Instant createdAt) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Seller name is required");
    }
    if (ownerUserId == null) {
      throw new IllegalArgumentException("Owner user id is required");
    }
    if (createdAt == null) {
      throw new IllegalArgumentException("Created timestamp is required");
    }
    return new SellerStoreEntity(
        UUID.randomUUID(),
        name.trim(),
        ownerUserId,
        SellerApprovalStatus.PENDING_REVIEW,
        createdAt,
        createdAt);
  }

  public void approve(UUID reviewerUserId, String reviewNotes, Instant reviewedAt) {
    review(SellerApprovalStatus.APPROVED, reviewerUserId, reviewNotes, reviewedAt);
  }

  public void reject(UUID reviewerUserId, String reviewNotes, Instant reviewedAt) {
    review(SellerApprovalStatus.REJECTED, reviewerUserId, reviewNotes, reviewedAt);
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public UUID ownerUserId() {
    return ownerUserId;
  }

  public SellerApprovalStatus approvalStatus() {
    return approvalStatus;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant submittedAt() {
    return submittedAt;
  }

  public Instant reviewedAt() {
    return reviewedAt;
  }

  public UUID reviewedByUserId() {
    return reviewedByUserId;
  }

  public String reviewNotes() {
    return reviewNotes;
  }

  private void review(
      SellerApprovalStatus approvalStatus,
      UUID reviewerUserId,
      String reviewNotes,
      Instant reviewedAt) {
    if (reviewerUserId == null) {
      throw new IllegalArgumentException("Reviewer user id is required");
    }
    if (reviewedAt == null) {
      throw new IllegalArgumentException("Reviewed timestamp is required");
    }
    this.approvalStatus = approvalStatus;
    this.reviewedByUserId = reviewerUserId;
    this.reviewedAt = reviewedAt;
    this.reviewNotes = reviewNotes == null || reviewNotes.isBlank() ? null : reviewNotes.trim();
  }
}
