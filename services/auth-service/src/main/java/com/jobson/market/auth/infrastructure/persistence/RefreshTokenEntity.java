package com.jobson.market.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID familyId;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, unique = true)
  private String tokenHash;

  @Column private UUID previousTokenId;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column private Instant usedAt;

  @Column private Instant revokedAt;

  protected RefreshTokenEntity() {}

  RefreshTokenEntity(
      UUID id,
      UUID familyId,
      UUID userId,
      String tokenHash,
      UUID previousTokenId,
      Instant expiresAt,
      Instant createdAt) {
    this.id = id;
    this.familyId = familyId;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.previousTokenId = previousTokenId;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  UUID familyId() {
    return familyId;
  }

  UUID userId() {
    return userId;
  }

  String tokenHash() {
    return tokenHash;
  }

  UUID previousTokenId() {
    return previousTokenId;
  }

  Instant expiresAt() {
    return expiresAt;
  }

  Instant usedAt() {
    return usedAt;
  }

  Instant revokedAt() {
    return revokedAt;
  }

  void markUsed(Instant usedAt) {
    this.usedAt = usedAt;
  }

  void revoke(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }
}
