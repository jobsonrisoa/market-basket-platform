package com.jobson.market.customer_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfileEntity {

  @Id private UUID id;

  @Column(name = "auth_user_id", nullable = false, unique = true)
  private UUID authUserId;

  @Column(name = "display_name")
  private String displayName;

  @Column private String phone;

  @Column(name = "default_locale", nullable = false)
  private String defaultLocale;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CustomerProfileStatus status;

  @Column(name = "address_preferences", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String addressPreferences;

  @Column(name = "communication_preferences", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String communicationPreferences;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CustomerProfileEntity() {}

  private CustomerProfileEntity(UUID id, UUID authUserId, Instant createdAt) {
    this.id = id;
    this.authUserId = authUserId;
    this.defaultLocale = "en-US";
    this.status = CustomerProfileStatus.ACTIVE;
    this.addressPreferences = "{}";
    this.communicationPreferences = "{}";
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public static CustomerProfileEntity initialFor(UUID authUserId, Instant createdAt) {
    if (authUserId == null) {
      throw new IllegalArgumentException("Auth user id is required");
    }
    if (createdAt == null) {
      throw new IllegalArgumentException("Created timestamp is required");
    }
    return new CustomerProfileEntity(UUID.randomUUID(), authUserId, createdAt);
  }

  public void update(String displayName, String phone, String defaultLocale, Instant updatedAt) {
    if (updatedAt == null) {
      throw new IllegalArgumentException("Updated timestamp is required");
    }
    this.displayName = blankToNull(displayName);
    this.phone = blankToNull(phone);
    this.defaultLocale = normalizeLocale(defaultLocale);
    this.updatedAt = updatedAt;
  }

  public UUID id() {
    return id;
  }

  public UUID authUserId() {
    return authUserId;
  }

  public String displayName() {
    return displayName;
  }

  public String phone() {
    return phone;
  }

  public String defaultLocale() {
    return defaultLocale;
  }

  public CustomerProfileStatus status() {
    return status;
  }

  public String addressPreferences() {
    return addressPreferences;
  }

  public String communicationPreferences() {
    return communicationPreferences;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String normalizeLocale(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? "en-US" : normalized;
  }
}
