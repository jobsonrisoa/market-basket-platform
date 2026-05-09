package com.jobson.market.auth.infrastructure.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerSubject"}))
class OAuthAccountEntity {

  @Id private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String provider;

  @Column(nullable = false)
  private String providerSubject;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private Instant createdAt;

  protected OAuthAccountEntity() {}

  OAuthAccountEntity(
      UUID id,
      UUID userId,
      String provider,
      String providerSubject,
      String email,
      Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.provider = provider;
    this.providerSubject = providerSubject;
    this.email = email;
    this.createdAt = createdAt;
  }

  UUID id() {
    return id;
  }

  UUID userId() {
    return userId;
  }

  String provider() {
    return provider;
  }

  String providerSubject() {
    return providerSubject;
  }

  String email() {
    return email;
  }
}
