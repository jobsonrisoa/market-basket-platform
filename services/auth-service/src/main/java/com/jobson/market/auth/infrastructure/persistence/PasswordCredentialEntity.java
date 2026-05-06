package com.jobson.market.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_credentials")
class PasswordCredentialEntity {

  @Id private UUID userId;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private Instant changedAt;

  protected PasswordCredentialEntity() {}

  PasswordCredentialEntity(UUID userId, String passwordHash, Instant changedAt) {
    this.userId = userId;
    this.passwordHash = passwordHash;
    this.changedAt = changedAt;
  }

  String passwordHash() {
    return passwordHash;
  }
}
