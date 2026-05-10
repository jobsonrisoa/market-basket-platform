package com.jobson.market.catalog_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "catalog_products")
public class ProductEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false)
  private UUID sellerId;

  @Column(nullable = false)
  private UUID categoryId;

  @Column(nullable = false)
  private String name;

  @Column private String description;

  @Column(nullable = false)
  private String unit;

  @Column(nullable = false)
  private String packageSize;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal priceAmount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProductStatus status;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected ProductEntity() {}

  private ProductEntity(
      UUID id,
      UUID sellerId,
      UUID categoryId,
      String name,
      String description,
      String unit,
      String packageSize,
      BigDecimal priceAmount,
      String currency,
      ProductStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.sellerId = Objects.requireNonNull(sellerId, "sellerId is required");
    this.categoryId = Objects.requireNonNull(categoryId, "categoryId is required");
    this.name = requireText(name, "name");
    this.description = normalizeOptional(description);
    this.unit = requireText(unit, "unit");
    this.packageSize = requireText(packageSize, "packageSize");
    this.priceAmount = requirePrice(priceAmount);
    this.currency = requireCurrency(currency);
    this.status = Objects.requireNonNull(status, "status is required");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
  }

  public static ProductEntity create(
      UUID sellerId,
      UUID categoryId,
      String name,
      String description,
      String unit,
      String packageSize,
      BigDecimal priceAmount,
      String currency,
      Instant now) {
    return new ProductEntity(
        UUID.randomUUID(),
        sellerId,
        categoryId,
        name,
        description,
        unit,
        packageSize,
        priceAmount,
        currency,
        ProductStatus.DRAFT,
        now,
        now);
  }

  public void update(
      UUID categoryId,
      String name,
      String description,
      String unit,
      String packageSize,
      BigDecimal priceAmount,
      String currency,
      Instant now) {
    this.categoryId = Objects.requireNonNull(categoryId, "categoryId is required");
    this.name = requireText(name, "name");
    this.description = normalizeOptional(description);
    this.unit = requireText(unit, "unit");
    this.packageSize = requireText(packageSize, "packageSize");
    this.priceAmount = requirePrice(priceAmount);
    this.currency = requireCurrency(currency);
    this.updatedAt = Objects.requireNonNull(now, "updatedAt is required");
  }

  public void publish(Instant now) {
    this.status = ProductStatus.PUBLISHED;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt is required");
  }

  public void unpublish(Instant now) {
    this.status = ProductStatus.UNPUBLISHED;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt is required");
  }

  public UUID id() {
    return id;
  }

  public UUID sellerId() {
    return sellerId;
  }

  public UUID categoryId() {
    return categoryId;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public String unit() {
    return unit;
  }

  public String packageSize() {
    return packageSize;
  }

  public BigDecimal priceAmount() {
    return priceAmount;
  }

  public String currency() {
    return currency;
  }

  public ProductStatus status() {
    return status;
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

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static BigDecimal requirePrice(BigDecimal value) {
    Objects.requireNonNull(value, "priceAmount is required");
    if (value.signum() < 0) {
      throw new IllegalArgumentException("priceAmount must be non-negative");
    }
    return value;
  }

  private static String requireCurrency(String value) {
    String currency = requireText(value, "currency").toUpperCase();
    if (currency.length() != 3) {
      throw new IllegalArgumentException("currency must be a three-letter ISO code");
    }
    return currency;
  }
}
