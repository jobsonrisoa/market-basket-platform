package com.jobson.market.seller_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @Column(nullable = false)
  private Instant createdAt;

  protected SellerStoreEntity() {}

  private SellerStoreEntity(UUID id, String name, UUID ownerUserId, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.ownerUserId = ownerUserId;
    this.createdAt = createdAt;
  }

  public static SellerStoreEntity create(String name, UUID ownerUserId, Instant createdAt) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Seller name is required");
    }
    if (ownerUserId == null) {
      throw new IllegalArgumentException("Owner user id is required");
    }
    return new SellerStoreEntity(UUID.randomUUID(), name.trim(), ownerUserId, createdAt);
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

  public Instant createdAt() {
    return createdAt;
  }
}
