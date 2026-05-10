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
@Table(name = "seller_memberships")
public class SellerMembershipEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SellerMembershipRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SellerMembershipStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column private Instant removedAt;

  protected SellerMembershipEntity() {}

  private SellerMembershipEntity(
      UUID id,
      UUID sellerId,
      UUID userId,
      SellerMembershipRole role,
      SellerMembershipStatus status,
      Instant createdAt,
      Instant removedAt) {
    this.id = id;
    this.sellerId = sellerId;
    this.userId = userId;
    this.role = role;
    this.status = status;
    this.createdAt = createdAt;
    this.removedAt = removedAt;
  }

  public static SellerMembershipEntity active(
      UUID sellerId, UUID userId, SellerMembershipRole role, Instant createdAt) {
    return new SellerMembershipEntity(
        UUID.randomUUID(),
        requireId(sellerId, "Seller id is required"),
        requireId(userId, "User id is required"),
        requireRole(role),
        SellerMembershipStatus.ACTIVE,
        createdAt,
        null);
  }

  public void activateAs(SellerMembershipRole role) {
    this.role = requireRole(role);
    this.status = SellerMembershipStatus.ACTIVE;
    this.removedAt = null;
  }

  public void remove(Instant removedAt) {
    this.status = SellerMembershipStatus.REMOVED;
    this.removedAt = removedAt;
  }

  public UUID id() {
    return id;
  }

  public UUID sellerId() {
    return sellerId;
  }

  public UUID userId() {
    return userId;
  }

  public SellerMembershipRole role() {
    return role;
  }

  public SellerMembershipStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant removedAt() {
    return removedAt;
  }

  private static UUID requireId(UUID id, String message) {
    if (id == null) {
      throw new IllegalArgumentException(message);
    }
    return id;
  }

  private static SellerMembershipRole requireRole(SellerMembershipRole role) {
    if (role == null) {
      throw new IllegalArgumentException("Membership role is required");
    }
    return role;
  }
}
