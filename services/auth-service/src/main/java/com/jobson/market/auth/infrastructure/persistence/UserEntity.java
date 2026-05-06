package com.jobson.market.auth.infrastructure.persistence;

import com.jobson.market.auth.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private boolean emailVerified;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected UserEntity() {}

  UserEntity(
      UUID id,
      String email,
      boolean emailVerified,
      UserStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.emailVerified = emailVerified;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  UUID id() {
    return id;
  }

  String email() {
    return email;
  }

  boolean emailVerified() {
    return emailVerified;
  }

  UserStatus status() {
    return status;
  }
}
