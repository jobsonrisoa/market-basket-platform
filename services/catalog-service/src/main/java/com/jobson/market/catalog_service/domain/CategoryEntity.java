package com.jobson.market.catalog_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_categories")
public class CategoryEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CategoryEntity() {}

  private CategoryEntity(UUID id, String name, Instant createdAt, Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.name = requireText(name, "name");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
  }

  public static CategoryEntity create(String name, Instant now) {
    return new CategoryEntity(UUID.randomUUID(), name, now, now);
  }

  public void rename(String name, Instant now) {
    this.name = requireText(name, "name");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt is required");
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Instant createdAt() {
    return createdAt;
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
