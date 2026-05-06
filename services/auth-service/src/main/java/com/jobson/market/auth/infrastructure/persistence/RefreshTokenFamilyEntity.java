package com.jobson.market.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_families")
class RefreshTokenFamilyEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String status;

  @Column private Instant revokedAt;

  @Column private String revokeReason;

  @Column(nullable = false)
  private Instant createdAt;

  protected RefreshTokenFamilyEntity() {}

  RefreshTokenFamilyEntity(UUID id, UUID userId, String status, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.status = status;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  UUID userId() {
    return userId;
  }

  boolean active() {
    return "ACTIVE".equals(status);
  }

  void revoke(Instant revokedAt, String reason) {
    this.status = "REVOKED";
    this.revokedAt = revokedAt;
    this.revokeReason = reason;
  }
}
